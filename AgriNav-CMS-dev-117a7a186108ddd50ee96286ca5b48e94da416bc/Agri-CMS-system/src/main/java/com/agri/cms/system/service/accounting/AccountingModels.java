package com.agri.cms.system.service.accounting;

import com.agri.cms.system.service.impl.AreaCalculationServiceImpl.CoverageBreakdown;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.util.List;

public final class AccountingModels {
    private AccountingModels() { }
    public record Coordinate(Double longitude, Double latitude) { }
    public record PreviewRequest(Long plotId, @JsonDeserialize(using = WholeCentimetresDeserializer.class) Integer widthCm,
                                 List<List<Coordinate>> segments) { }
    public record Result(String trajectoryId, String plotId, String source, String coordinateSystem,
                         boolean preview, int widthCm, int segmentCount, int pointCount,
                         CoverageBreakdown calculation, BigDecimal recordedAreaMu,
                         BigDecimal differenceFromRecordedAreaMu, List<String> warnings) { }
}
