package com.agri.cms.system.service.geojson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

/** 每条记录按输入顺序返回；READY 不表示已检查数据库编码或已经入库。 */
public class GeoJsonImportResult {
    private final boolean dryRun;
    private boolean databaseChecked;
    private int importedCount;
    private final List<Item> items = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    @JsonIgnore
    private final List<ParsedGeoJsonPlot> plots = new ArrayList<>();

    public GeoJsonImportResult(boolean dryRun) { this.dryRun = dryRun; }
    public boolean isDryRun() { return dryRun; }
    public boolean isDatabaseChecked() { return databaseChecked; }
    public void setDatabaseChecked(boolean value) { databaseChecked = value; }
    public int getTotalCount() { return items.size(); }
    public long getInvalidCount() { return items.stream().filter(i -> "INVALID".equals(i.status)).count(); }
    public long getValidCount() { return getTotalCount() - getInvalidCount(); }
    public int getImportedCount() { return importedCount; }
    public List<Item> getItems() { return items; }
    public List<String> getWarnings() { return warnings; }
    @JsonIgnore public List<ParsedGeoJsonPlot> getPlots() { return plots; }
    public void imported() { importedCount++; }

    public static class Item {
        private final int index;
        private final String plotCode;
        private final String landName;
        private String status;
        private String message;
        private String plotId;
        public Item(int index, String plotCode, String landName, String status, String message) {
            this.index=index; this.plotCode=plotCode; this.landName=landName;
            this.status=status; this.message=message;
        }
        public int getIndex() { return index; }
        public String getPlotCode() { return plotCode; }
        public String getLandName() { return landName; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getPlotId() { return plotId; }
        public void invalid(String reason) { status="INVALID"; message=reason; }
        public void imported(long id) { status="IMPORTED"; message="导入成功"; plotId=String.valueOf(id); }
    }
}