package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
import static com.agri.cms.system.service.playback.PlaybackModels.*;

/** 独立 scroll 查询，不使用旧接口固定 request-size；所有 scroll 在 finally 中释放。 */
@Repository
public class RawPlaybackReader {
    private static final Logger LOG=LoggerFactory.getLogger(RawPlaybackReader.class);
    private static final RequestOptions READ_OPTIONS=RequestOptions.DEFAULT.toBuilder().setRequestConfig(
            RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(5000).setSocketTimeout(20000).build()).build();
    private final RestHighLevelClient client;
    private final String index;
    public RawPlaybackReader(RestHighLevelClient client,@Value("${spring.elasticsearch.index}") String index) {this.client=client;this.index=index;}

    public List<Sample> read(String phone,LocalDateTime start,LocalDateTime end) {
        List<Sample> points=new ArrayList<>();String scrollId=null;long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(60);
        try {
            // match_phrase 支持既有 text 映射，随后逐条校验手机号完全相等，避免模糊命中。
            var query=QueryBuilders.boolQuery().filter(QueryBuilders.matchPhraseQuery("phoneNumber",phone))
                    .filter(QueryBuilders.rangeQuery("deviceTime").gte(start.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString())
                            .lte(end.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString()));
            var source=new SearchSourceBuilder().query(query).size(1000).timeout(TimeValue.timeValueSeconds(15)).sort("deviceTime",SortOrder.ASC).sort("_doc",SortOrder.ASC);
            var request=new SearchRequest(index).source(source).scroll(TimeValue.timeValueMinutes(1));
            SearchResponse response=client.search(request,READ_OPTIONS);
            int scanned=0;
            while(true) {
                if(response.getScrollId()!=null)scrollId=response.getScrollId();
                if(System.nanoTime()>deadline)throw new ServiceException("原始轨迹读取耗时过长，请缩小时间范围后重试");
                if(response.isTimedOut()||response.getFailedShards()>0)throw new ServiceException("原始轨迹查询不完整，请稍后重试");
                SearchHit[] hits=response.getHits().getHits();if(hits.length==0)break;
                for(SearchHit hit:hits) {
                    if(++scanned>TrajectoryPlaybackService.MAX_POINTS)throw new ServiceException("轨迹超过50000点，请缩小时间范围");
                    Map<String,Object> row=hit.getSourceAsMap();
                    if(!phone.equals(String.valueOf(row.get("phoneNumber"))))throw new ServiceException("原始轨迹终端匹配异常，已拒绝返回数据");
                    points.add(decode(row));
                }
                if(scrollId==null)throw new ServiceException("原始轨迹分页上下文缺失，无法确认数据完整性");
                response=client.scroll(new SearchScrollRequest(scrollId).scroll(TimeValue.timeValueMinutes(1)),READ_OPTIONS);
            }
            return points;
        } catch(ServiceException failure){throw failure;}
        catch(Exception failure){LOG.warn("原始回放读取失败",failure);throw new ServiceException("原始轨迹数据源暂不可用，请稍后重试");}
        finally {
            if(scrollId!=null)try {var clear=new ClearScrollRequest();clear.addScrollId(scrollId);client.clearScroll(clear,READ_OPTIONS);}
            catch(Exception cleanup){LOG.warn("原始回放 scroll 清理失败",cleanup);}
        }
    }

    static Sample decode(Map<String,Object> row) {
        try {
            // GPS 接入端将原始协议整数微度写入 ES；有效轨迹则已经是十进制度。
            double lon=Double.parseDouble(String.valueOf(row.get("longitude")))/1_000_000d;
            double lat=Double.parseDouble(String.valueOf(row.get("latitude")))/1_000_000d;
            LocalDateTime time=ZonedDateTime.parse(String.valueOf(row.get("deviceTime"))).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
            return new Sample(lon,lat,time);
        } catch(RuntimeException invalid){return new Sample(Double.NaN,Double.NaN,null);}
    }
}
