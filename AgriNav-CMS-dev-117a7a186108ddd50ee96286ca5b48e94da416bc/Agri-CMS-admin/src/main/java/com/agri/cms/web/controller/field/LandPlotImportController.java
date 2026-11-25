package com.agri.cms.web.controller.field;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.geojson.GeoJsonImportResult;
import com.agri.cms.system.service.geojson.GeoJsonLandPlotImportService;
import com.agri.cms.system.service.geojson.GeoJsonPlotParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;

@Tag(name="地块文件导入")
@RestController
@RequestMapping("/landPlot/import")
public class LandPlotImportController extends BaseController {
    private final GeoJsonLandPlotImportService importer;
    public LandPlotImportController(GeoJsonLandPlotImportService importer) { this.importer=importer; }

    /** file 为 .geojson / .json；dryRun 默认为 true，仅检查文件，不写库或验证库内编码。 */
    @PostMapping(value="/geojson",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermi('field:square:save')")
    @Operation(summary="GeoJSON 地块导入或文件预检（默认仅预检）")
    public AjaxResult importGeoJson(@RequestParam("file") MultipartFile file,
                                   @RequestParam(defaultValue="true") boolean dryRun) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > GeoJsonPlotParser.MAX_BYTES) {
            return AjaxResult.error("请选择不超过 10MB 的 GeoJSON 文件");
        }
        try (InputStream stream=file.getInputStream()) {
            GeoJsonImportResult result=dryRun ? importer.preview(stream) : importer.importPlots(stream,getUserId());
            if (result.getInvalidCount() > 0) {
                return AjaxResult.error("存在无效地块，未写入任何地块；请查看逐条校验结果",result);
            }
            return AjaxResult.success(dryRun ? "文件预检通过，尚未检查库内编码，也未导入" : "地块已导入",result);
        } catch (IllegalArgumentException invalid) {
            return AjaxResult.error(invalid.getMessage());
        }
    }
}