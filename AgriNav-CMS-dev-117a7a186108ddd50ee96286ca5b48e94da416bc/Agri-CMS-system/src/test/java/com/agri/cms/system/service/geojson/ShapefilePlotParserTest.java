package com.agri.cms.system.service.geojson;

import org.junit.Test;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ShapefilePlotParserTest {
    private final ShapefilePlotParser parser=new ShapefilePlotParser(new GeoJsonPlotParser());
    private GeoJsonImportResult parse(Map<String,byte[]> files) throws Exception {
        return parser.parse(new ByteArrayInputStream(ShapefileTestData.zip(files)),true,"UTF-8");
    }
    @Test public void validZipPreservesKnownCoordinatesChineseAndSource() throws Exception {
        var result=parse(ShapefileTestData.files(StandardCharsets.UTF_8,"SHP001"));
        assertEquals(0,result.getInvalidCount());assertEquals("示例农田",result.getPlots().get(0).landName());
        assertEquals("Shapefile",result.getPlots().get(0).sourceFormat());assertEquals(4326,result.getPlots().get(0).geometry().getSRID());
        assertEquals(.000001,result.getPlots().get(0).geometry().getArea(),1e-12);assertTrue(result.isDryRun());
    }
    @Test public void gbkCpgOverridesDefaultUtf8() throws Exception {
        var result=parse(ShapefileTestData.files(Charset.forName("GBK"),"GBK001"));assertEquals("测试村",result.getPlots().get(0).address());
    }
    @Test public void missingCpgCanUseExplicitEncoding() throws Exception {
        var files=ShapefileTestData.files(Charset.forName("GBK"),"GBK001");files.remove("plots.cpg");
        var result=parser.parse(new ByteArrayInputStream(ShapefileTestData.zip(files)),true,"GBK");assertEquals(0,result.getInvalidCount());
    }
    @Test public void missingSidecarsAreRejected() throws Exception {
        for(String extension:List.of("shp","shx","dbf","prj")) {
            var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.remove("plots."+extension);
            assertThrows(IllegalArgumentException.class,()->parse(files));
        }
    }
    @Test public void repeatedCodesUseSameGeoJsonValidation() throws Exception {
        var result=parse(ShapefileTestData.files(StandardCharsets.UTF_8,"A","A"));assertEquals(2,result.getInvalidCount());
    }
    @Test public void multipleDatasetsAreRejected() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put("another.shp",files.get("plots.shp"));
        assertThrows(IllegalArgumentException.class,()->parse(files));
    }
    @Test public void folderAndUpperCaseExtensionsAreAccepted() throws Exception {
        Map<String,byte[]> renamed=new LinkedHashMap<>();ShapefileTestData.files(StandardCharsets.UTF_8,"A").forEach((name,bytes)->renamed.put("folder/"+name.toUpperCase(Locale.ROOT),bytes));
        assertEquals(0,parse(renamed).getInvalidCount());
    }
    @Test public void traversalPathsAreRejectedEvenForUnusedFiles() throws Exception {
        for(String path:List.of("../escape.txt","folder/../../escape.txt","C:\\escape.shp","/absolute.shp")) {
            var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put(path,new byte[]{1});
            assertThrows(IllegalArgumentException.class,()->parse(files));
        }
    }
    @Test public void sameNameInDifferentDirectoriesIsRejected() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put("other/PLOTS.SHP",files.get("plots.shp"));
        assertThrows(IllegalArgumentException.class,()->parse(files));
    }
    @Test public void invalidProjectionIsNotSilentlyRelabeled() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put("plots.prj","EPSG:3857".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,()->parse(files));
    }
    @Test public void unsupportedEncodingIsRejected() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put("plots.cpg","UTF-16".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,()->parse(files));
    }
    @Test public void validButDifferentDatumIsRejected() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");
        files.put("plots.prj",ShapefileTestData.WGS84.replace("WGS 84","CGCS2000").replace("WGS_1984","China_2000").replace("298.257223563","298.257222101").replace("4326","4490").getBytes(StandardCharsets.UTF_8));
        var error=assertThrows(IllegalArgumentException.class,()->parse(files));assertTrue(error.getMessage().contains("WGS84"));
    }
    @Test public void holesAndMultiplePolygonsCannotBypassCommonValidation() throws Exception {
        double[][] outer={{114,38},{114,38.01},{114.01,38.01},{114.01,38},{114,38}};
        double[][] hole={{114.002,38.002},{114.008,38.002},{114.008,38.008},{114.002,38.008},{114.002,38.002}};
        double[][] second={{114.02,38},{114.02,38.01},{114.03,38.01},{114.03,38},{114.02,38}};
        assertEquals(1,parse(ShapefileTestData.polygonRings(outer,hole)).getInvalidCount());
        assertEquals(1,parse(ShapefileTestData.polygonRings(outer,second)).getInvalidCount());
    }
    @Test public void mismatchedDbfCountIsRejected() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");ByteBuffer.wrap(files.get("plots.dbf")).order(ByteOrder.LITTLE_ENDIAN).putInt(4,2);
        assertThrows(IllegalArgumentException.class,()->parse(files));
    }
    @Test public void truncatedShapeAndCorruptIndexAreRejected() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put("plots.shp",Arrays.copyOf(files.get("plots.shp"),120));assertThrows(IllegalArgumentException.class,()->parse(files));
        var files2=ShapefileTestData.files(StandardCharsets.UTF_8,"A");ByteBuffer.wrap(files2.get("plots.shx")).putInt(100,99);assertThrows(IllegalArgumentException.class,()->parse(files2));
    }
    @Test public void maliciousPointCountIsRejectedBeforeLibraryAllocation() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");ByteBuffer.wrap(files.get("plots.shp")).order(ByteOrder.LITTLE_ENDIAN).putInt(148,Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,()->parse(files));
    }
    @Test public void entryExpansionAndEntryCountAreLimited() throws Exception {
        var files=ShapefileTestData.files(StandardCharsets.UTF_8,"A");files.put("junk.bin",new byte[17*1024*1024]);assertThrows(IllegalArgumentException.class,()->parse(files));
        var many=ShapefileTestData.files(StandardCharsets.UTF_8,"A");for(int i=0;i<101;i++)many.put("file"+i+".txt",new byte[0]);assertThrows(IllegalArgumentException.class,()->parse(many));
    }
    @Test public void tempDirectoriesAreRemovedOnSuccessAndFailure() throws Exception {
        var temp=Path.of(System.getProperty("java.io.tmpdir"));Set<Path> before;
        try(var paths=Files.list(temp)){before=new HashSet<>(paths.filter(p->p.getFileName().toString().startsWith("agrinav-shp-")).toList());}
        parse(ShapefileTestData.files(StandardCharsets.UTF_8,"A"));var broken=ShapefileTestData.files(StandardCharsets.UTF_8,"A");broken.remove("plots.prj");assertThrows(IllegalArgumentException.class,()->parse(broken));
        try(var paths=Files.list(temp)){assertEquals(before,new HashSet<>(paths.filter(p->p.getFileName().toString().startsWith("agrinav-shp-")).toList()));}
    }
    @Test public void emptyOrNonZipInputIsRejected() {
        assertThrows(IllegalArgumentException.class,()->parser.parse(new ByteArrayInputStream(new byte[0]),true,"UTF-8"));
        assertThrows(IllegalArgumentException.class,()->parser.parse(new ByteArrayInputStream("not zip".getBytes(StandardCharsets.UTF_8)),true,"UTF-8"));
    }
}
