package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.junit.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 本机假 ES HTTP 服务验证真实客户端的分页与释放；不连接团队 ES。 */
public class RawPlaybackReaderTest {
    private final LocalDateTime start=LocalDateTime.of(2026,11,23,8,0);
    @Test public void decodesProtocolMicroDegreesAndConvertsUtcToShanghai() {
        var point=RawPlaybackReader.decode(Map.of("longitude",114000000,"latitude","38000000","deviceTime","2026-11-23T00:00:00Z"));assertEquals(114,point.longitude(),1e-9);assertEquals(38,point.latitude(),1e-9);assertEquals(start,point.time());
    }
    @Test public void badTimeOrCoordinateIsMarkedForVisibleValidation() {
        assertNull(RawPlaybackReader.decode(Map.of("longitude","oops")).time());
    }
    private String page(String hits,boolean timeout) {
        return "{\"_scroll_id\":\"cursor\",\"took\":1,\"timed_out\":"+timeout+",\"_shards\":{\"total\":1,\"successful\":1,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":{\"value\":2,\"relation\":\"eq\"},\"max_score\":null,\"hits\":["+hits+"]}}";
    }
    private String hit(String id,String phone) {
        return "{\"_index\":\"gps\",\"_id\":\""+id+"\",\"_score\":null,\"_source\":{\"longitude\":114000000,\"latitude\":38000000,\"phoneNumber\":\""+phone+"\",\"deviceTime\":\"2026-11-23T00:00:00Z\"}}";
    }
    private void exercise(boolean wrongPhone,boolean timeout) throws Exception {
        var cleared=new AtomicBoolean();var scrolls=new AtomicInteger();var queryBody=new AtomicReference<String>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            String path=exchange.getRequestURI().getPath(),method=exchange.getRequestMethod(),body;
            String input=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            if(method.equals("DELETE")){cleared.set(true);body="{\"succeeded\":true,\"num_freed\":1}";}
            else if(path.equals("/")){body="{\"name\":\"test\",\"cluster_name\":\"test\",\"cluster_uuid\":\"test\",\"version\":{\"number\":\"7.17.9\",\"build_flavor\":\"default\",\"build_type\":\"tar\",\"build_hash\":\"test\",\"build_date\":\"2023-01-01T00:00:00Z\",\"build_snapshot\":false,\"lucene_version\":\"8.11.1\",\"minimum_wire_compatibility_version\":\"6.8.0\",\"minimum_index_compatibility_version\":\"6.0.0-beta1\"},\"tagline\":\"You Know, for Search\"}";}
            else if(path.contains("_search/scroll")){body=scrolls.incrementAndGet()==1?page(hit("2","15888888888"),false):page("",false);}
            else {queryBody.set(input);body=page(hit("1",wrongPhone?"13900000000":"15888888888"),timeout);}
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().add("Content-Type","application/json");exchange.getResponseHeaders().add("X-Elastic-Product","Elasticsearch");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try(var client=new RestHighLevelClient(RestClient.builder(new HttpHost("127.0.0.1",server.getAddress().getPort(),"http")))) {
            var reader=new RawPlaybackReader(client,"gps");
            if(wrongPhone||timeout)assertThrows(ServiceException.class,()->reader.read("15888888888",start,start.plusHours(1)));
            else {assertEquals(2,reader.read("15888888888",start,start.plusHours(1)).size());assertEquals(2,scrolls.get());assertTrue(queryBody.get().contains("deviceTime"));assertTrue(queryBody.get().contains("15888888888"));}
            assertTrue("scroll must be released on success and failure",cleared.get());
        } finally {server.stop(0);}
    }
    @Test public void readsMultiplePagesAndAlwaysClearsScroll() throws Exception {exercise(false,false);}
    @Test public void rejectsMismatchedPhoneAndClearsScroll() throws Exception {exercise(true,false);}
    @Test public void timedOutResponsesCannotMasqueradeAsCompleteAndClearScroll() throws Exception {exercise(false,true);}
}
