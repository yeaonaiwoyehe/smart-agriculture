package com.agri.cms.system.service.geojson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * B-01 首版：支持 Feature / FeatureCollection 中的无内环 Polygon。
 * 现有 GPS 的地块点判定只读取外环；内环、MultiPolygon 必须明确拒绝，不能静默丢失。
 * 坐标按 RFC 7946：WGS84 [经度,纬度]；不把其他坐标系简单重标为 4326。
 */
@Component
public class GeoJsonPlotParser {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final int MAX_FEATURES = 1000;
    private static final int MAX_POSITIONS = 100000;
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    public GeoJsonImportResult parse(InputStream input, boolean dryRun) {
        if (input == null) throw new IllegalArgumentException("请选择 GeoJSON 文件");
        JsonNode root;
        try {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw new IllegalArgumentException("文件不能为空，且不能超过 10MB");
            }
            root = json.readTree(bytes);
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取有效的 GeoJSON，请检查 JSON 格式及 UTF-8 编码", exception);
        }
        if (root == null || !root.isObject()) throw new IllegalArgumentException("GeoJSON 根节点必须为对象");
        checkCrs(root);
        List<JsonNode> features = new ArrayList<>();
        if ("Feature".equals(root.path("type").asText())) features.add(root);
        else if ("FeatureCollection".equals(root.path("type").asText()) && root.path("features").isArray()) {
            root.path("features").forEach(features::add);
        } else throw new IllegalArgumentException("请选择 Feature 或 FeatureCollection 格式的地块文件");
        if (features.isEmpty() || features.size() > MAX_FEATURES) {
            throw new IllegalArgumentException("每次需导入 1 至 1000 个地块");
        }
        GeoJsonImportResult result = new GeoJsonImportResult(dryRun);
        Map<String, GeoJsonImportResult.Item> codes = new HashMap<>();
        int positions = 0;
        for (int i = 0; i < features.size(); i++) {
            JsonNode feature = features.get(i);
            JsonNode properties = feature.path("properties");
            String code = null, name = null;
            try {
                if (!"Feature".equals(feature.path("type").asText()) || !properties.isObject()) {
                    throw new IllegalArgumentException("每条地块必须是含 properties 的 Feature");
                }
                checkCrs(feature);
                code = text(properties, "plotCode", "地块编号", 64, true);
                name = text(properties, "landName", "地块名称", 100, true);
                String address = text(properties, "address", "地址", 255, true);
                String landType = text(properties, "landType", "地块类型", 50, false);
                if (landType == null) landType = "普通地";
                if ("插花地".equals(landType)) {
                    throw new IllegalArgumentException("插花地需单独维护排除区域，首版导入请选择普通地块");
                }
                JsonNode geometry = feature.path("geometry");
                checkCrs(geometry);
                if (!"Polygon".equals(geometry.path("type").asText())) {
                    throw new IllegalArgumentException("首版仅支持 Polygon；多面地块请先拆分并分别赋码");
                }
                JsonNode rings = geometry.path("coordinates");
                if (!rings.isArray() || rings.size() != 1 || !rings.get(0).isArray()) {
                    throw new IllegalArgumentException("首版仅支持无内环 Polygon，不能丢弃空洞后导入");
                }
                JsonNode ring = rings.get(0);
                positions += ring.size();
                if (positions > MAX_POSITIONS) throw new IllegalArgumentException("文件累计坐标点超过 100000 个");
                if (ring.size() < 4) throw new IllegalArgumentException("边界至少需要四个坐标，首尾必须相同");
                Coordinate[] coordinates = new Coordinate[ring.size()];
                for (int j = 0; j < ring.size(); j++) {
                    JsonNode position = ring.get(j);
                    if (!position.isArray() || (position.size() != 2 && position.size() != 3)
                            || !position.get(0).isNumber() || !position.get(1).isNumber()
                            || (position.size() == 3 && (!position.get(2).isNumber()
                            || !Double.isFinite(position.get(2).doubleValue())))) {
                        throw new IllegalArgumentException("每个坐标必须为数字数组 [经度,纬度]，可带数值高度");
                    }
                    double lon = position.get(0).doubleValue(), lat = position.get(1).doubleValue();
                    if (!Double.isFinite(lon) || !Double.isFinite(lat) || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                        throw new IllegalArgumentException("经纬度越界或不是有限数值，必须使用 WGS84");
                    }
                    coordinates[j] = new Coordinate(lon, lat);
                }
                if (!coordinates[0].equals2D(coordinates[coordinates.length - 1])) {
                    throw new IllegalArgumentException("边界没有闭合：最后一个坐标必须与第一个相同");
                }
                Polygon polygon = factory.createPolygon(coordinates);
                IsValidOp validity = new IsValidOp(polygon);
                if (!validity.isValid() || polygon.getArea() <= 0) {
                    throw new IllegalArgumentException("边界自相交、退化或几何无效");
                }
                if (polygon.getEnvelopeInternal().getWidth() >= 180) {
                    throw new IllegalArgumentException("不支持跨日期变更线的地块，请先拆分");
                }
                GeoJsonImportResult.Item item = new GeoJsonImportResult.Item(i + 1, code, name, "READY", "文件校验通过，尚未检查库内编码");
                GeoJsonImportResult.Item previous = codes.putIfAbsent(code, item);
                if (previous != null) {
                    previous.invalid("文件内地块编码重复");
                    item.invalid("文件内地块编码重复");
                }
                result.getItems().add(item);
                result.getPlots().add(new ParsedGeoJsonPlot(i + 1, code, name, address, landType, polygon));
            } catch (IllegalArgumentException exception) {
                result.getItems().add(new GeoJsonImportResult.Item(i + 1, code, name, "INVALID", exception.getMessage()));
            }
        }
        return result;
    }

    private String text(JsonNode properties, String key, String alias, int limit, boolean required) {
        JsonNode value = properties.has(key) ? properties.get(key) : properties.get(alias);
        if (value == null || value.isNull()) {
            if (required) throw new IllegalArgumentException("缺少属性 " + key + "（" + alias + "）");
            return null;
        }
        if (!value.isTextual()) throw new IllegalArgumentException(key + " 必须为文本");
        String text = value.textValue().trim();
        if (text.isEmpty() || text.length() > limit || text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(key + " 不能为空、包含控制字符或超过 " + limit + " 字符");
        }
        return text;
    }

    private void checkCrs(JsonNode node) {
        if (!node.has("crs")) return; // RFC 7946 未声明即为 WGS84。
        JsonNode crs = node.path("crs");
        String name = crs.path("properties").path("name").asText();
        if (!"name".equals(crs.path("type").asText()) || !Set.of("EPSG:4326", "urn:ogc:def:crs:OGC:1.3:CRS84",
                "urn:ogc:def:crs:EPSG::4326", "OGC:CRS84").contains(name)) {
            throw new IllegalArgumentException("只接受 WGS84 / SRID 4326；请先转换坐标系，不能仅修改标记");
        }
    }
}