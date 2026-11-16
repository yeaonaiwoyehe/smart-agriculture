package com.agri.cms.common.core.domain;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-18 15:24
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 15:24
 */
public class DebeziumMessage<T> {
    /** 变更前的数据 */
    private T before;
    
    /** 变更后的数据 */
    private T after;
    
    /** 源数据信息 */
    private Source source;
    
    /** 操作类型 (c=create/u=update/d=delete) */
    private String op;
    
    /** 事件时间戳 */
    private Long ts_ms;
    
    /** 事务信息 */
    private Object transaction;

    public static class Source {
        private String version;
        private String connector;
        private String name;
        private Long ts_ms;
        private String snapshot;
        private String db;
        private String sequence;
        private String table;
        private Integer server_id;
        private String gtid;
        private String file;
        private Long pos;
        private Integer row;
        private Integer thread;
        private String query;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getConnector() {
            return connector;
        }

        public void setConnector(String connector) {
            this.connector = connector;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getTs_ms() {
            return ts_ms;
        }

        public void setTs_ms(Long ts_ms) {
            this.ts_ms = ts_ms;
        }

        public String getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(String snapshot) {
            this.snapshot = snapshot;
        }

        public String getDb() {
            return db;
        }

        public void setDb(String db) {
            this.db = db;
        }

        public String getSequence() {
            return sequence;
        }

        public void setSequence(String sequence) {
            this.sequence = sequence;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public Integer getServer_id() {
            return server_id;
        }

        public void setServer_id(Integer server_id) {
            this.server_id = server_id;
        }

        public String getGtid() {
            return gtid;
        }

        public void setGtid(String gtid) {
            this.gtid = gtid;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public Long getPos() {
            return pos;
        }

        public void setPos(Long pos) {
            this.pos = pos;
        }

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public Integer getThread() {
            return thread;
        }

        public void setThread(Integer thread) {
            this.thread = thread;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        @Override
        public String toString() {
            return "Source{" +
                    "version='" + version + '\'' +
                    ", connector='" + connector + '\'' +
                    ", name='" + name + '\'' +
                    ", ts_ms=" + ts_ms +
                    ", snapshot='" + snapshot + '\'' +
                    ", db='" + db + '\'' +
                    ", sequence='" + sequence + '\'' +
                    ", table='" + table + '\'' +
                    ", server_id=" + server_id +
                    ", gtid='" + gtid + '\'' +
                    ", file='" + file + '\'' +
                    ", pos=" + pos +
                    ", row=" + row +
                    ", thread=" + thread +
                    ", query='" + query + '\'' +
                    '}';
        }
    }

    // getter和setter方法
    public T getBefore() {
        return before;
    }

    public void setBefore(T before) {
        this.before = before;
    }

    public T getAfter() {
        return after;
    }

    public void setAfter(T after) {
        this.after = after;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Long getTs_ms() {
        return ts_ms;
    }

    public void setTs_ms(Long ts_ms) {
        this.ts_ms = ts_ms;
    }

    public Object getTransaction() {
        return transaction;
    }

    public void setTransaction(Object transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {
        return "DebeziumMessage{" +
                "before=" + before +
                ", after=" + after +
                ", source=" + source +
                ", op='" + op + '\'' +
                ", ts_ms=" + ts_ms +
                ", transaction=" + transaction +
                '}';
    }
}

