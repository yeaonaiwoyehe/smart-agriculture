package org.yzh.web.config;

import org.yzh.protocol.commons.transform.AttributeKey;

import java.util.Map;

public class Constants {
    public static final Map<Integer, String> AddtionalInfo = Map.ofEntries(
            Map.entry(0x01, "Mileage"),
            Map.entry(0x02, "Fuel"),
            Map.entry(0x03, "Speed"),
            Map.entry(0x04, "AlarmEventId"),
            Map.entry(0x05, "TirePressure"),
            Map.entry(0x06, "CarriageTemperature"),
            Map.entry(0x11, "OverSpeedAlarm"),
            Map.entry(0x12, "InOutAreaAlarm"),
            Map.entry(0x13, "RouteDriveTimeAlarm"),
            Map.entry(0x14, "VideoRelatedAlarm"),
            Map.entry(0x15, "VideoMissingStatus"),
            Map.entry(0x16, "VideoObscuredStatus"),
            Map.entry(0x17, "StorageFailureStatus"),
            Map.entry(0x18, "DriverBehaviorAlarm"),
            Map.entry(0x25, "Signal"),
            Map.entry(0x2A, "IoState"),
            Map.entry(0x2B, "AnalogQuantity"),
            Map.entry(0x30, "SignalStrength"),
            Map.entry(0x31, "GnssCount"),
            Map.entry(0x64, "AlarmADAS"),
            Map.entry(0x65, "AlarmDSM"),
            Map.entry(0x66, "AlarmTPMS"),
            Map.entry(0x67, "AlarmBSD"),
            Map.entry(0xF1, "InstallErrorMsg"),
            Map.entry(0xF2, "AlgorithmErrorMsg"),
            Map.entry(0xEE, "WorkType"),
            Map.entry(0xFF, "Width")
    );
    public static int notDeleted = 0;
}