package com.agri.cms.system.domain.record;

import com.agri.cms.common.utils.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 10:56
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 10:56
 */
@Document(indexName = "land_plot")
public record LandPlotRecord(
        @Id
        @Field(name = "id",type = FieldType.Keyword)
        String esId,

        @Field(name = "land_id",type = FieldType.Keyword)
        String landId,
        @Field(name = "land_name",type = FieldType.Text)
        String landName,
        @Field(name = "land_type",type = FieldType.Text)
        String landType,
        @Field(name = "land_area",type = FieldType.Double)
        Float landArea,
        @Field(name = "is_deleted",type = FieldType.Integer)
        int isDeleted,
        @Field(name = "address",type = FieldType.Text)
        String address,
        @Field(name = "create_by",type = FieldType.Text)
        String createBy,
        @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "Asia/Shanghai")
        LocalDateTime createTime,
        @Field(name = "update_by",type = FieldType.Text)
        String updateBy,
        @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "Asia/Shanghai")
        LocalDateTime updateTime,
        @Field(name = "image",type = FieldType.Text)
        String image,
        @Field(name = "measurement_method",type = FieldType.Text)
        String measurementMethod,
        @Field(name = "boundary",type = FieldType.Keyword)
        Polygon boundary,
        @Field(name = "center_point",type = FieldType.Keyword)
        Point centerPoint,
        @Field(name = "owner_name",type = FieldType.Text)
        String ownerName,
        @Field(name = "owner_phone",type = FieldType.Text)
        String ownerPhone
) {
        public LandPlotRecord(com.agri.cms.system.domain.LandPlot landPlot) {
                this(
                        UUID.randomUUID().toString(),
                        landPlot.getId().toString(),
                        landPlot.getLandName(),
                        landPlot.getLandType(),
                        landPlot.getLandArea(),
                        landPlot.getIsDeleted(),
                        landPlot.getAddress(),
                        landPlot.getCreateBy(),
                        landPlot.getCreateTime(),
                        landPlot.getUpdateBy(),
                        landPlot.getUpdateTime(),
                        landPlot.getImage(),
                        landPlot.getMeasurementMethod(),
                       landPlot.getBoundary(),
                        landPlot.getCenterPoint(),
                        landPlot.getOwnerName(),
                        landPlot.getOwnerPhone()
                );
        }
}
