package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.service.impl.AreaCalculationServiceImpl;
import com.agri.cms.system.service.impl.BoundaryClippingService;
import org.junit.Test;

import java.util.List;

import static com.agri.cms.system.service.playback.TrajectoryAnomalyService.Kind.*;
import static org.junit.Assert.*;

public class TrajectoryAnomalyDemoTest {
    private final TrajectoryAnomalyDemo demo = new TrajectoryAnomalyDemo(
            new BoundaryClippingService(new AreaCalculationServiceImpl()));

    @Test public void originalFixtureExercisesAllFiveMarkers() {
        var result = demo.analyze("ORIGINAL");
        assertEquals("DEMO_FIXTURE", result.source());
        assertEquals(List.of(STOP, OVERSPEED, GPS_SPIKE, GAP, TRANSFER_CANDIDATE),
                result.events().stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertEquals(80.0, result.events().get(3).durationSeconds(), 0.0);
        assertEquals(80.0, result.events().get(4).maxDisplacementMetres(), 0.1);
    }

    @Test public void effectiveFixtureShowsAnArchiveGap() {
        var result = demo.analyze("EFFECTIVE");
        assertEquals(List.of(STOP, OVERSPEED, GPS_SPIKE, ARCHIVE_GAP),
                result.events().stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertThrows(ServiceException.class, () -> demo.analyze("INVALID"));
    }

    @Test public void demoWorkorderComputesPositiveAreasAndKeepsTheGapAndSpikeEdgesBroken() {
        var workorder = demo.workorder();
        assertEquals(12, workorder.track().size());
        assertEquals(List.of(6, 7, 8), workorder.breakBefore());
        assertTrue(workorder.effectiveAreaMu().signum() > 0);
        assertTrue(workorder.grossAreaMu().compareTo(workorder.effectiveAreaMu()) > 0);
        assertTrue(workorder.outsideAreaMu().signum() > 0);
    }
}
