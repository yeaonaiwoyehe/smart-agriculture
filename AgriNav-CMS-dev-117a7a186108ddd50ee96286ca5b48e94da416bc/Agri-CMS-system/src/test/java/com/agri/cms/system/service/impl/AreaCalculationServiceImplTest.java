package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class AreaCalculationServiceImplTest {
    private final AreaCalculationServiceImpl service = new AreaCalculationServiceImpl();
    // Fixtures specify metre offsets on the equator, independently of implementation helpers.
    static Point point(double east, double north) {
        return new Point(Math.toDegrees(east / 6371000.0), Math.toDegrees(north / 6371000.0));
    }
    static List<Point> points(double... east) {
        return Arrays.stream(east).mapToObj(x -> point(x, 0)).collect(java.util.stream.Collectors.toList());
    }
    private void area(double expected, List<Point> points, int width) {
        assertEquals(expected, service.calculateTrajectoryAreaAndDuplicateRemoval(points, width).doubleValue(), 0.02);
    }
    @Test public void straightHundredMetres() { area(300, points(0, 100), 300); }
    @Test public void exactReturnIsCountedOnce() { area(300, points(0, 100, 0), 300); }
    @Test public void partialRetracingKeepsNewCoverage() { area(450, points(0, 100, 50, 150), 300); }
    @Test public void closeDisjointSegmentsAreNotOverlap() { area(2.30, points(0, 1, 1.3, 2.3), 100); }
    @Test public void partiallyOverlappingSwathsUseWidth() {
        // 100x3 + 2x3 + 100x3, minus two 1.5m2 corner overlaps and 100m2 shared swath.
        area(503, Arrays.asList(point(0,0), point(100,0), point(100,2), point(0,2)), 300);
    }
    @Test public void repeatedPointsHaveNoArea() { area(0, points(0,0,0), 300); }
    @Test public void insufficientPointsHaveNoArea() {
        area(0, Collections.emptyList(), 300); area(0, points(0), 300);
    }
    @Test public void invalidWidthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.calculateTrajectoryAreaAndDuplicateRemoval(points(0,100), -1));
        assertThrows(IllegalArgumentException.class, () -> service.calculateTrajectoryAreaAndDuplicateRemoval(points(0,100), 0));
    }
    @Test public void nullAndInvalidCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.calculateTrajectoryAreaAndDuplicateRemoval(null, 300));
        assertThrows(IllegalArgumentException.class, () -> service.calculateTrajectoryAreaAndDuplicateRemoval(Arrays.asList(point(0,0), null), 300));
        for (Point invalid : Arrays.asList(new Point(Double.NaN,0), new Point(181,0), new Point(0,91), new Point(0,Double.POSITIVE_INFINITY))) {
            assertThrows(IllegalArgumentException.class, () -> service.calculateTrajectoryAreaAndDuplicateRemoval(Arrays.asList(point(0,0),invalid), 300));
        }
    }
    @Test public void basicLengthMethodRetainsItsContract() {
        Trajectory trajectory = new Trajectory(); trajectory.setWidth(300); trajectory.setPoints(points(0,100,0));
        assertEquals(600, service.calculateTrajectoryArea(trajectory).doubleValue(), 0.02);
        assertThrows(IllegalArgumentException.class, () -> service.calculateTrajectoryArea(null));
    }
}
