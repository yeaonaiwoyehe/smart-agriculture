package com.agri.cms.framework.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.postgis.LineString;
import org.postgis.Point;
import org.postgis.Polygon;

import java.io.IOException;


/**
 * @author 姚锦伦
 */
public class GeometrySerializer {

    static class PolygonSerializer extends JsonSerializer<Polygon> {
        @Override
        public void serialize(Polygon polygon, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            StringBuffer sb = new StringBuffer();
            polygon.outerWKT(sb);
            jsonGenerator.writeString(sb.toString());
        }

    }

    static class PointSerializer extends JsonSerializer<Point> {
        @Override
        public void serialize(Point point, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            StringBuffer sb = new StringBuffer();
            point.outerWKT(sb);
            jsonGenerator.writeString(sb.toString());
        }

    }
    static class LineStringSerializer extends JsonSerializer<LineString> {
        @Override
        public void serialize(LineString lineString, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            StringBuffer sb = new StringBuffer();
            lineString.outerWKT(sb);
            jsonGenerator.writeString(sb.toString());
        }
    }
}
