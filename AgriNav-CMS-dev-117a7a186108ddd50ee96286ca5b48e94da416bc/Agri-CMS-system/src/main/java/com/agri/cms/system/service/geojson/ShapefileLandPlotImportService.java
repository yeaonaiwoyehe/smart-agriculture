package com.agri.cms.system.service.geojson;

import org.springframework.stereotype.Service;
import java.io.InputStream;

/** 先解析并关闭临时文件，再进入公共入库服务的 PostgreSQL 事务。 */
@Service
public class ShapefileLandPlotImportService {
    private final ShapefilePlotParser parser;
    private final GeoJsonLandPlotImportService importer;
    public ShapefileLandPlotImportService(ShapefilePlotParser parser,GeoJsonLandPlotImportService importer) {this.parser=parser;this.importer=importer;}
    public GeoJsonImportResult process(InputStream file,boolean dryRun,String encoding,long userId) {
        GeoJsonImportResult result=parser.parse(file,dryRun,encoding);
        return dryRun?result:importer.importValidated(result,userId);
    }
}