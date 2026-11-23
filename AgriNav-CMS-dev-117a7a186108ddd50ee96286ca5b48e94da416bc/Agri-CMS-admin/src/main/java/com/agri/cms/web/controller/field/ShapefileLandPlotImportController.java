package com.agri.cms.web.controller.field;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.geojson.GeoJsonPlotParser;
import com.agri.cms.system.service.geojson.ShapefileLandPlotImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Tag(name="地块文件导入")
@RestController
@RequestMapping("/landPlot/import")
public class ShapefileLandPlotImportController extends BaseController {
    private final ShapefileLandPlotImportService importer;
    public ShapefileLandPlotImportController(ShapefileLandPlotImportService importer) {this.importer=importer;}

    @PostMapping(value="/shapefile",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermi('field:square:save')")
    @Operation(summary="Shapefile ZIP 预检或导入（默认仅预检）")
    public AjaxResult importShapefile(@RequestParam("file") MultipartFile file,
                                     @RequestParam(defaultValue="true") boolean dryRun,
                                     @RequestParam(defaultValue="UTF-8") String encoding) throws IOException {
        if(file==null||file.isEmpty()||file.getSize()>GeoJsonPlotParser.MAX_BYTES)
            return AjaxResult.error("请选择不超过 10MB 的 Shapefile ZIP 文件");
        try(var input=file.getInputStream()) {
            var result=importer.process(input,dryRun,encoding,dryRun?0:getUserId());
            if(result.getInvalidCount()>0)return AjaxResult.error("存在无效地块，未写入任何地块；请查看逐条校验结果",result);
            return AjaxResult.success(dryRun?"文件预检通过，尚未检查库内编码，也未导入":"地块已导入",result);
        } catch(IllegalArgumentException invalid) {return AjaxResult.error(invalid.getMessage());}
    }
}
