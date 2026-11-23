package com.agri.cms.system.service.geojson;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.mapper.GeoJsonLandPlotImportMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.locationtech.jts.geom.Point;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@DS("pg")
public class GeoJsonLandPlotImportService {
    private final GeoJsonPlotParser parser;
    private final GeoJsonLandPlotImportMapper mapper;
    private final RedisCache redis;
    public GeoJsonLandPlotImportService(GeoJsonPlotParser parser, GeoJsonLandPlotImportMapper mapper, RedisCache redis) {
        this.parser=parser; this.mapper=mapper; this.redis=redis;
    }

    /** 文件预检不连接数据库；不声称库内编码可用。 */
    public GeoJsonImportResult preview(InputStream file) {
        return parser.parse(file, true);
    }

    /** 地块、编码关联和包围盒同事务；任意写入失败则整批回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public GeoJsonImportResult importPlots(InputStream file, long userId) {
        if (userId <= 0) throw new IllegalArgumentException("导入人身份无效");
        GeoJsonImportResult result=parser.parse(file,false);
        if (result.getInvalidCount() != 0) return result;
        if (!mapper.hasCodeTable()) {
            throw new IllegalStateException("请先由团队确认并应用 database/geojson_import_code_ddl.sql，再正式导入");
        }
        List<String> codes=result.getPlots().stream().map(ParsedGeoJsonPlot::plotCode).collect(Collectors.toList());
        Set<String> existing=new HashSet<>(mapper.existingCodes(codes));
        result.setDatabaseChecked(true);
        for (GeoJsonImportResult.Item item:result.getItems()) {
            if (existing.contains(item.getPlotCode())) item.invalid("编码已被使用，整批未导入");
        }
        if (result.getInvalidCount() != 0) return result;
        Map<Long,Point> centres=new LinkedHashMap<>();
        for (ParsedGeoJsonPlot plot:result.getPlots()) {
            long id=IdWorker.getId();
            Point centre=plot.geometry().getInteriorPoint();
            if (mapper.insertPlot(id,plot,plot.geometry().toText(),centre.toText(),String.valueOf(userId)) != 1
                    || mapper.insertBoundary(id) != 1) {
                throw new IllegalStateException("地块或包围盒保存失败，整批导入已回滚");
            }
            try {
                if (mapper.insertCode(plot.plotCode(),id,String.valueOf(userId)) != 1) {
                    throw new IllegalStateException("地块编码保存失败，整批导入已回滚");
                }
            } catch (DuplicateKeyException conflict) {
                throw new IllegalStateException("地块编码被其他导入占用，请重新预检；整批导入已回滚",conflict);
            }
            centres.put(id,centre);
            result.getItems().get(plot.index()-1).imported(id);
            result.imported();
        }
        // Redis 无法参与 PostgreSQL 事务；仅在数据库提交后更新，避免出现幽灵地块。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { refreshLocationIndex(centres,result); }
            });
        } else {
            throw new IllegalStateException("导入必须在事务内执行");
        }
        return result;
    }

    private void refreshLocationIndex(Map<Long,Point> centres,GeoJsonImportResult result) {
        for (Map.Entry<Long,Point> entry:centres.entrySet()) {
            try {
                Point point=entry.getValue();
                redis.setCacheGeoValue(Constants.LAND_PLOT_GEO_KEY,
                        new org.springframework.data.geo.Point(point.getX(),point.getY()),entry.getKey());
            } catch (RuntimeException failure) {
                if (result.getWarnings().isEmpty()) {
                    result.getWarnings().add("地块已入库，但位置索引更新失败；列表可查询，附近地块查询需重建索引后使用");
                }
            }
        }
    }
}