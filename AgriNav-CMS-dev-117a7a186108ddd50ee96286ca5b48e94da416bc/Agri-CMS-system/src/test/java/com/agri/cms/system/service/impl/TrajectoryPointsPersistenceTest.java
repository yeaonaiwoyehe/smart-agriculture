package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.TrajectoryPoints;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import org.junit.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.Assert.*;

public class TrajectoryPointsPersistenceTest {
    private static class Storage extends TrajectoryPointsServiceImpl {
        boolean result;
        Collection<TrajectoryPoints> saved;
        @Override public boolean saveBatch(Collection<TrajectoryPoints> values) {saved=values; return result;}
    }
    private Trajectory trajectory() {
        Trajectory trajectory=new Trajectory();
        trajectory.setPoints(Arrays.asList(new Point(114.123456789,38,LocalDateTime.now()),
                new Point(114.124,38,LocalDateTime.now()),new Point(114.124,38.001,LocalDateTime.now())));
        return trajectory;
    }
    @Test public void failedBatchWriteIsReportedToCaller() {
        Storage storage=new Storage();storage.result=false;
        assertFalse(storage.savePoints(trajectory(),UUID.randomUUID()));
    }
    @Test public void storageExceptionsPropagateToPreventBufferCleanup() {
        Storage storage=new Storage(){@Override public boolean saveBatch(Collection<TrajectoryPoints> rows){throw new IllegalStateException("database unavailable");}};
        assertThrows(IllegalStateException.class,()->storage.savePoints(trajectory(),UUID.randomUUID()));
    }
    @Test public void localeDoesNotCorruptCoordinatesOrLosePrecision() {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);Storage storage=new Storage();storage.result=true;
            assertTrue(storage.savePoints(trajectory(),UUID.randomUUID()));
            assertEquals(114.123456789,storage.saved.iterator().next().getPoints().getRing(0).getPoint(0).getX(),1e-10);
        } finally {Locale.setDefault(previous);}
    }
}
