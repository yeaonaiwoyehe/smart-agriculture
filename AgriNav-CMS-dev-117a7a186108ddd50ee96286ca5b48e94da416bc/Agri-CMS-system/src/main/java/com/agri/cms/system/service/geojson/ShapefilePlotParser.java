package com.agri.cms.system.service.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** ZIP 内仅接收一组同名 Shapefile；使用 GeoTools 解析，复用 GeoJSON 业务校验。 */
@Component
public class ShapefilePlotParser {
    private static final Logger LOG=LoggerFactory.getLogger(ShapefilePlotParser.class);
    private static final long MAX_EXPANDED=32L*1024*1024;
    private static final long MAX_ENTRY=16L*1024*1024;
    private static final Set<String> EXTENSIONS=Set.of("shp","shx","dbf","prj","cpg");
    private final GeoJsonPlotParser validator;
    private final ObjectMapper json=new ObjectMapper();
    public ShapefilePlotParser(GeoJsonPlotParser validator) {this.validator=validator;}

    public GeoJsonImportResult parse(InputStream input,boolean dryRun,String encoding) {
        Path directory=null;
        try {
            if(input==null)throw new IllegalArgumentException("请选择 Shapefile ZIP 文件");
            byte[] archive=input.readNBytes(GeoJsonPlotParser.MAX_BYTES+1);
            if(archive.length<4||archive.length>GeoJsonPlotParser.MAX_BYTES||archive[0]!='P'||archive[1]!='K')
                throw new IllegalArgumentException("请选择不超过 10MB 的有效 ZIP 压缩包");
            directory=Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")),"agrinav-shp-");
            Map<String,Path> files=extract(archive,directory);
            Path shape=files.values().stream().filter(p->p.toString().endsWith(".shp")).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("ZIP 中没有 .shp 文件"));
            String name=shape.getFileName().toString();String stem=name.substring(0,name.length()-4);
            for(String ext:List.of("shp","shx","dbf","prj"))if(!files.containsKey(stem+"."+ext))
                throw new IllegalArgumentException("缺少同名 ."+ext+" 文件；请一起打包 shp、shx、dbf、prj");
            for(String file:files.keySet())if(!file.substring(0,file.lastIndexOf('.')).equals(stem))
                throw new IllegalArgumentException("每次仅支持一组同名 Shapefile，请分开打包上传");
            validateHeaders(shape,files.get(stem+".shx"),files.get(stem+".dbf"));
            Charset charset=charset(encoding);
            Path cpg=files.get(stem+".cpg");
            if(cpg!=null) {
                if(Files.size(cpg)>256)throw new IllegalArgumentException("CPG 编码说明过长");
                charset=charset(Files.readString(cpg,StandardCharsets.UTF_8).replace("\uFEFF","").trim());
            }
            Path projection=files.get(stem+".prj");
            if(Files.size(projection)>65536)throw new IllegalArgumentException("PRJ 坐标系说明过长");
            var crs=CRS.parseWKT(Files.readString(projection,StandardCharsets.UTF_8).replace("\uFEFF","").trim());
            if(!CRS.equalsIgnoreMetadata(crs,DefaultGeographicCRS.WGS84))
                throw new IllegalArgumentException("PRJ 不是 WGS84 经纬度坐标系，请先在 GIS 软件中转换为 EPSG:4326 后导入");
            Map<String,Object> parameters=new HashMap<>();parameters.put("url",shape.toUri().toURL());
            parameters.put("charset",charset);parameters.put("create spatial index",false);
            parameters.put("enable spatial index",false);parameters.put("memory mapped buffer",false);
            ShapefileDataStore store=(ShapefileDataStore)new ShapefileDataStoreFactory().createDataStore(parameters);
            ObjectNode collection=json.createObjectNode();collection.put("type","FeatureCollection");
            ArrayNode features=collection.putArray("features");
            try {
                try(SimpleFeatureIterator iterator=store.getFeatureSource().getFeatures().features()) {
                    while(iterator.hasNext()) {
                        if(features.size()>=GeoJsonPlotParser.MAX_FEATURES)throw new IllegalArgumentException("每次最多导入 1000 个地块");
                        var feature=iterator.next();ObjectNode item=features.addObject();item.put("type","Feature");
                        ObjectNode props=item.putObject("properties");
                        for(var descriptor:feature.getFeatureType().getAttributeDescriptors()) {
                            String field=descriptor.getLocalName();String key=property(field);
                            if(key==null)continue;
                            if(props.has(key))throw new IllegalArgumentException("属性列重复映射："+key);
                            Object value=feature.getAttribute(field);
                            if(value instanceof String text&&text.indexOf('\uFFFD')>=0)
                                throw new IllegalArgumentException("中文属性解码失败，请检查 .cpg 或选择正确文字编码");
                            props.set(key,json.valueToTree(value));
                        }
                        Object raw=feature.getDefaultGeometry();
                        Geometry geometry=raw instanceof Geometry?(Geometry)raw:null;
                        if(geometry instanceof MultiPolygon multi&&multi.getNumGeometries()==1)geometry=multi.getGeometryN(0);
                        ObjectNode geo=item.putObject("geometry");
                        if(geometry instanceof Polygon polygon) {
                            geo.put("type","Polygon");ArrayNode rings=geo.putArray("coordinates");
                            ring(rings,polygon.getExteriorRing());
                            for(int i=0;i<polygon.getNumInteriorRing();i++)ring(rings,polygon.getInteriorRingN(i));
                        } else geo.put("type",geometry==null?"Null":geometry.getGeometryType());
                    }
                }
            } finally {store.dispose();}
            GeoJsonImportResult result=validator.parse(new ByteArrayInputStream(json.writeValueAsBytes(collection)),dryRun);
            result.getPlots().replaceAll(p->new ParsedGeoJsonPlot(p.index(),p.plotCode(),p.landName(),p.address(),p.landType(),p.geometry(),"Shapefile"));
            return result;
        } catch(IllegalArgumentException invalid) {throw invalid;}
        catch(Exception failure) {throw new IllegalArgumentException("无法读取 Shapefile，请检查配套文件、PRJ 和文字编码是否正确",failure);}
        finally {
            if(directory!=null)try(var paths=Files.walk(directory)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
            } catch(IOException cleanup) {LOG.warn("Shapefile 临时文件清理失败",cleanup);}
        }
    }

    private Map<String,Path> extract(byte[] archive,Path directory) throws IOException {
        Map<String,Path> files=new HashMap<>();long total=0;int count=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(archive),StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if(++count>100)throw new IllegalArgumentException("压缩包条目过多，最多 100 个");
                String path=entry.getName().replace('\\','/');
                if(path.startsWith("/")||path.contains(":")||Arrays.asList(path.split("/")).contains(".."))
                    throw new IllegalArgumentException("压缩包包含非法路径");
                if(entry.isDirectory())continue;
                String name=path.substring(path.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
                String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1):"";
                boolean keep=EXTENSIONS.contains(ext)&&!path.startsWith("__MACOSX/")&&!name.startsWith("._");
                if(keep&&files.containsKey(name))throw new IllegalArgumentException("压缩包包含重复文件名："+name);
                Path target=directory.resolve(name).normalize();
                if(keep&&!target.startsWith(directory))throw new IllegalArgumentException("非法解压路径");
                try(OutputStream output=keep?Files.newOutputStream(target,StandardOpenOption.CREATE_NEW):OutputStream.nullOutputStream()) {
                    byte[] buffer=new byte[8192];long size=0;int read;
                    while((read=zip.read(buffer))!=-1) {
                        size+=read;total+=read;
                        if(size>MAX_ENTRY||total>MAX_EXPANDED)throw new IllegalArgumentException("解压后文件过大：单文件最多 16MB，合计最多 32MB");
                        output.write(buffer,0,read);
                    }
                }
                if(keep)files.put(name,target);
            }
        }
        return files;
    }

    /** 在交给 GIS 库前校验长度、记录数和点数，避免损坏文件声明超大数组。 */
    private void validateHeaders(Path shpPath,Path shxPath,Path dbfPath) throws IOException {
        byte[] shp=Files.readAllBytes(shpPath),shx=Files.readAllBytes(shxPath),dbf=Files.readAllBytes(dbfPath);
        for(byte[] file:List.of(shp,shx)) {
            if(file.length<100||ByteBuffer.wrap(file).getInt(0)!=9994||Integer.toUnsignedLong(ByteBuffer.wrap(file).getInt(24))*2!=file.length
                    ||ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN).getInt(28)!=1000)
                throw new IllegalArgumentException("SHP/SHX 文件头损坏或长度不一致");
        }
        if((shx.length-100)%8!=0)throw new IllegalArgumentException("SHX 索引长度无效");
        int count=0,offset=100,pointCount=0;
        while(offset<shp.length) {
            if(offset+12>shp.length||++count>1000)throw new IllegalArgumentException("SHP 记录损坏或超过 1000 条");
            long bytes=Integer.toUnsignedLong(ByteBuffer.wrap(shp).getInt(offset+4))*2;
            if(bytes<4||offset+8+bytes>shp.length)throw new IllegalArgumentException("SHP 记录长度无效");
            int index=100+(count-1)*8;
            if(index+8>shx.length||ByteBuffer.wrap(shx).getInt(index)*2L!=offset||ByteBuffer.wrap(shx).getInt(index+4)*2L!=bytes)
                throw new IllegalArgumentException("SHX 索引与 SHP 记录不匹配");
            ByteBuffer body=ByteBuffer.wrap(shp,offset+8,(int)bytes).slice().order(ByteOrder.LITTLE_ENDIAN);
            int type=body.getInt(0);
            if(type!=0) {
                if(!Set.of(5,15,25).contains(type)||bytes<44)throw new IllegalArgumentException("只支持地块多边形 Shapefile");
                int parts=body.getInt(36),points=body.getInt(40);
                if(parts<1||points<4||parts>points||44L+4L*parts+16L*points>bytes||points>100000-pointCount)
                    throw new IllegalArgumentException("SHP 分段或点数无效，累计不能超过 100000 点");
                pointCount+=points;int previous=-1;
                for(int i=0;i<parts;i++) {
                    int start=body.getInt(44+i*4);
                    if(start<=previous||start>=points||(i==0&&start!=0))throw new IllegalArgumentException("SHP 环索引无效");
                    previous=start;
                }
            }
            offset+=(int)bytes+8;
        }
        if(count!=(shx.length-100)/8||dbf.length<33)throw new IllegalArgumentException("SHP/SHX/DBF 配套记录不一致");
        ByteBuffer db=ByteBuffer.wrap(dbf).order(ByteOrder.LITTLE_ENDIAN);
        long rows=Integer.toUnsignedLong(db.getInt(4));int header=Short.toUnsignedInt(db.getShort(8)),width=Short.toUnsignedInt(db.getShort(10));
        if(rows!=count||header<33||header>dbf.length||(header-33)%32!=0||width<1||header+rows*width>dbf.length)
            throw new IllegalArgumentException("DBF 记录数量或长度与地块文件不匹配");
    }
    private Charset charset(String input) {
        String name=(input==null?"UTF-8":input).trim().toUpperCase(Locale.ROOT);
        return switch(name) {
            case "UTF-8","UTF8","65001"->StandardCharsets.UTF_8;
            case "GBK","CP936","936"->Charset.forName("GBK");
            case "GB18030"->Charset.forName("GB18030");
            default->throw new IllegalArgumentException("文字编码仅支持 UTF-8、GBK 或 GB18030");
        };
    }
    private String property(String name) {
        return switch(name.toLowerCase(Locale.ROOT)) {
            case "plotcode","plot_code","地块编号"->"plotCode";
            case "landname","land_name","地块名称"->"landName";
            case "address","地址"->"address";
            case "landtype","land_type","地块类型"->"landType";
            default->null;
        };
    }
    private void ring(ArrayNode rings,LineString line) {
        ArrayNode array=rings.addArray();for(Coordinate c:line.getCoordinates())array.addArray().add(c.x).add(c.y);
    }
}