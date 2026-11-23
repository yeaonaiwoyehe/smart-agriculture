package com.agri.cms.system.service.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class GeoJsonPlotParserTest {
    static final ObjectMapper JSON=new ObjectMapper();
    static ObjectNode feature(String code) {
        ObjectNode feature=JSON.createObjectNode();feature.put("type","Feature");
        feature.putObject("properties").put("plotCode",code).put("landName","测试田").put("address","测试村");
        ObjectNode geometry=feature.putObject("geometry");geometry.put("type","Polygon");
        ArrayNode ring=geometry.putArray("coordinates").addArray();
        for(double[] xy:new double[][]{{114,38},{114.001,38},{114.001,38.001},{114,38.001},{114,38}}) ring.addArray().add(xy[0]).add(xy[1]);
        return feature;
    }
    static ByteArrayInputStream file(ObjectNode... features) {
        ObjectNode collection=JSON.createObjectNode();collection.put("type","FeatureCollection");
        ArrayNode array=collection.putArray("features");for(ObjectNode f:features)array.add(f);
        return bytes(collection.toString());
    }
    static ByteArrayInputStream bytes(String content) {return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));}
    private final GeoJsonPlotParser parser=new GeoJsonPlotParser();
    private GeoJsonImportResult parse(ObjectNode feature) {return parser.parse(file(feature),true);}
    @Test public void parsesSingleFeatureAndCollectionInWgs84() {
        GeoJsonImportResult result=parse(feature("CODE-01"));
        assertEquals(0,result.getInvalidCount());assertEquals(1,result.getTotalCount());
        assertEquals(4326,result.getPlots().get(0).geometry().getSRID());
        assertEquals(114,result.getPlots().get(0).geometry().getCoordinate().x,0);
        assertEquals(0,parser.parse(bytes(feature("CODE-01").toString()),true).getInvalidCount());
    }
    @Test public void canonicalAndChinesePropertyNamesAreAccepted() {
        ObjectNode f=feature("unused");f.putObject("properties").put("地块编号","中文001").put("地块名称","一号地").put("地址","一村");
        assertEquals("中文001",parse(f).getPlots().get(0).plotCode());
    }
    @Test public void whitespaceIsTrimmedBeforeDuplicateCheck() {
        GeoJsonImportResult result=parser.parse(file(feature(" ABC "),feature("ABC")),true);
        assertEquals(2,result.getInvalidCount());assertEquals("文件内地块编码重复",result.getItems().get(0).getMessage());
    }
    @Test public void malformedJsonDuplicateKeysAndTrailingContentAreRejected() {
        for(String invalid:new String[]{"", "not json", "{} {}", "{\"type\":\"Feature\",\"type\":\"Feature\"}"})
            assertThrows(IllegalArgumentException.class,()->parser.parse(bytes(invalid),true));
    }
    @Test public void projectedCoordinateSystemsAreRejectedAtAnyLevel() {
        ObjectNode f=feature("A");f.putObject("crs").put("type","name").putObject("properties").put("name","EPSG:3857");
        assertEquals(1,parse(f).getInvalidCount());
        ObjectNode root=JSON.createObjectNode();root.put("type","FeatureCollection");root.putArray("features").add(feature("A"));
        root.putObject("crs").put("type","name").putObject("properties").put("name","GCJ-02");
        assertThrows(IllegalArgumentException.class,()->parser.parse(bytes(root.toString()),true));
    }
    @Test public void declaredWgs84IsAccepted() {
        ObjectNode f=feature("A");f.putObject("crs").put("type","name").putObject("properties").put("name","EPSG:4326");
        assertEquals(0,parse(f).getInvalidCount());
    }
    @Test public void missingCodeNameAndAddressAreReportedPerFeature() {
        for(String key:new String[]{"plotCode","landName","address"}) {
            ObjectNode f=feature("A");((ObjectNode)f.get("properties")).remove(key);assertEquals(1,parse(f).getInvalidCount());
        }
    }
    @Test public void propertyTypesLengthAndControlCharactersAreValidated() {
        ObjectNode f=feature("A");((ObjectNode)f.get("properties")).put("plotCode",123);assertEquals(1,parse(f).getInvalidCount());
        assertEquals(1,parse(feature("A".repeat(65))).getInvalidCount());assertEquals(1,parse(feature("A\nB")).getInvalidCount());
    }
    @Test public void unclosedRingIsRejectedRatherThanRepairedSilently() {
        ObjectNode f=feature("A");((ArrayNode)f.at("/geometry/coordinates/0")).remove(4);assertEquals(1,parse(f).getInvalidCount());
    }
    @Test public void selfIntersectionAndDegenerateRingAreRejected() {
        ObjectNode f=feature("A");ArrayNode ring=(ArrayNode)f.at("/geometry/coordinates/0");
        ring.removeAll();for(double[] xy:new double[][]{{0,0},{1,1},{0,1},{1,0},{0,0}})ring.addArray().add(xy[0]).add(xy[1]);
        assertEquals(1,parse(f).getInvalidCount());
        ring.removeAll();for(int i=0;i<4;i++)ring.addArray().add(0).add(0);assertEquals(1,parse(f).getInvalidCount());
    }
    @Test public void invalidCoordinatesAreRejected() {
        for(double value:new double[]{181,-181,Double.NaN,Double.POSITIVE_INFINITY}) {
            ObjectNode f=feature("A");((ArrayNode)f.at("/geometry/coordinates/0/0")).set(0,JSON.getNodeFactory().numberNode(value));
            assertEquals(1,parse(f).getInvalidCount());
        }
        ObjectNode f=feature("A");((ArrayNode)f.at("/geometry/coordinates/0/0")).set(1,JSON.getNodeFactory().numberNode(91));assertEquals(1,parse(f).getInvalidCount());
    }
    @Test public void multiPolygonAndHolesAreExplicitlyRejected() {
        ObjectNode f=feature("A");((ObjectNode)f.get("geometry")).put("type","MultiPolygon");assertEquals(1,parse(f).getInvalidCount());
        f=feature("A");ArrayNode rings=(ArrayNode)f.at("/geometry/coordinates");rings.add(rings.get(0).deepCopy());assertEquals(1,parse(f).getInvalidCount());
    }
    @Test public void heightMayBePresentButNeverUsedAsLatitude() {
        ObjectNode f=feature("A");for(var p:f.at("/geometry/coordinates/0"))((ArrayNode)p).add(500);
        assertEquals(0,parse(f).getInvalidCount());assertEquals(38,parse(f).getPlots().get(0).geometry().getCoordinate().y,0);
    }
    @Test public void emptyAndOversizedCollectionsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->parser.parse(file(),true));
        ObjectNode[] many=new ObjectNode[1001];java.util.Arrays.fill(many,feature("A"));
        assertThrows(IllegalArgumentException.class,()->parser.parse(file(many),true));
    }
    @Test public void oversizedStreamIsRejectedBeforeJsonParsing() {
        assertThrows(IllegalArgumentException.class,()->parser.parse(new ByteArrayInputStream(new byte[GeoJsonPlotParser.MAX_BYTES+1]),true));
    }
    @Test public void responseDoesNotSerializeInternalGeometryObjects() throws Exception {
        String response=JSON.writeValueAsString(parse(feature("A")));
        assertFalse(response.contains("\"plots\""));assertTrue(response.contains("\"databaseChecked\":false"));
    }
    @Test public void errorsKeepOriginalFeatureIndices() {
        ObjectNode invalid=feature("bad");((ObjectNode)invalid.get("properties")).remove("address");
        GeoJsonImportResult result=parser.parse(file(invalid,feature("good")),true);
        assertEquals(1,result.getInvalidCount());assertEquals(2,result.getPlots().get(0).index());
    }
}