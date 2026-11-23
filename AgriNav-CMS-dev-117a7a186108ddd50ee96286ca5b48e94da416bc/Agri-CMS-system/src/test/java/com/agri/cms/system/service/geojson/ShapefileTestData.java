package com.agri.cms.system.service.geojson;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** 独立按 ESRI 二进制格式生成小型已知矩形，不依赖被测 GeoTools 读取器。 */
public class ShapefileTestData {
    static final String WGS84="GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4326\"]]";
    static Map<String,byte[]> files(Charset encoding,String... codes) throws IOException {
        int count=codes.length,recordBytes=128;
        ByteBuffer shp=ByteBuffer.allocate(100+count*(8+recordBytes));header(shp,5);
        ByteBuffer shx=ByteBuffer.allocate(100+count*8);header(shx,5);
        for(int i=0;i<count;i++) {
            double x=114+i*.002;int offset=shp.position();
            shx.order(ByteOrder.BIG_ENDIAN).putInt(offset/2).putInt(recordBytes/2);
            shp.order(ByteOrder.BIG_ENDIAN).putInt(i+1).putInt(recordBytes/2);
            shp.order(ByteOrder.LITTLE_ENDIAN).putInt(5).putDouble(x).putDouble(38).putDouble(x+.001).putDouble(38.001);
            shp.putInt(1).putInt(5).putInt(0);
            for(double[] xy:new double[][]{{x,38},{x,38.001},{x+.001,38.001},{x+.001,38},{x,38}})shp.putDouble(xy[0]).putDouble(xy[1]);
        }
        String[] fields={"plotCode","landName","address","landType"};int[] widths={32,80,120,24};
        int header=32+32*fields.length+1,row=1+Arrays.stream(widths).sum();
        ByteBuffer dbf=ByteBuffer.allocate(header+count*row+1).order(ByteOrder.LITTLE_ENDIAN);
        dbf.put((byte)3).put((byte)126).put((byte)1).put((byte)1).putInt(count).putShort((short)header).putShort((short)row);dbf.position(32);
        for(int i=0;i<fields.length;i++) {
            int start=dbf.position();dbf.put(fields[i].getBytes(StandardCharsets.US_ASCII));dbf.position(start+11);dbf.put((byte)'C');dbf.position(start+16);dbf.put((byte)widths[i]);dbf.position(start+32);
        }
        dbf.put((byte)13);
        for(String code:codes) {
            dbf.put((byte)' ');String[] values={code,"示例农田","测试村","普通地"};
            for(int i=0;i<values.length;i++) {
                byte[] bytes=values[i].getBytes(encoding);dbf.put(bytes);for(int j=bytes.length;j<widths[i];j++)dbf.put((byte)' ');
            }
        }
        dbf.put((byte)26);
        Map<String,byte[]> files=new LinkedHashMap<>();files.put("plots.shp",shp.array());files.put("plots.shx",shx.array());files.put("plots.dbf",dbf.array());files.put("plots.prj",WGS84.getBytes(StandardCharsets.UTF_8));files.put("plots.cpg",encoding.name().getBytes(StandardCharsets.UTF_8));return files;
    }
    static void header(ByteBuffer buffer,int type) {
        buffer.order(ByteOrder.BIG_ENDIAN).putInt(9994);buffer.position(24);buffer.putInt(buffer.capacity()/2);
        buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(1000).putInt(type).putDouble(114).putDouble(38).putDouble(115).putDouble(39);buffer.position(100);
    }
    static Map<String,byte[]> polygonRings(double[][]... rings) throws IOException {
        var files=files(StandardCharsets.UTF_8,"RINGS1");
        int points=Arrays.stream(rings).mapToInt(r->r.length).sum(),length=44+4*rings.length+16*points;
        ByteBuffer shp=ByteBuffer.allocate(108+length);header(shp,5);
        shp.order(ByteOrder.BIG_ENDIAN).putInt(1).putInt(length/2);
        shp.order(ByteOrder.LITTLE_ENDIAN).putInt(5).putDouble(114).putDouble(38).putDouble(115).putDouble(39).putInt(rings.length).putInt(points);
        int offset=0;for(var ring:rings){shp.putInt(offset);offset+=ring.length;}
        for(var ring:rings)for(var xy:ring)shp.putDouble(xy[0]).putDouble(xy[1]);
        ByteBuffer shx=ByteBuffer.allocate(108);header(shx,5);shx.order(ByteOrder.BIG_ENDIAN).putInt(50).putInt(length/2);
        files.put("plots.shp",shp.array());files.put("plots.shx",shx.array());return files;
    }
    static byte[] zip(Map<String,byte[]> files) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)) {
            for(var file:files.entrySet()) {zip.putNextEntry(new ZipEntry(file.getKey()));zip.write(file.getValue());zip.closeEntry();}
        }return out.toByteArray();
    }
    /** 手动运行可生成不写数据库的网页演示文件。 */
    public static void main(String[] args) throws Exception {
        Files.write(Path.of(args[0]),zip(files(StandardCharsets.UTF_8,"DEVB-SHP-DEMO-001")));
    }
}
