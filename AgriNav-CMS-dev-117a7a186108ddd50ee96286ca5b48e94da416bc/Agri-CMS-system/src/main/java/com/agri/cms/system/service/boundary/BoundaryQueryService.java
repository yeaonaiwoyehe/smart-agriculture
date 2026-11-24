package com.agri.cms.system.service.boundary;
import com.agri.cms.ai.agent.mapper.WorkOrderGeoMapper;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.mapper.*;
import com.agri.cms.system.service.impl.BoundaryClippingService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.agri.cms.system.service.boundary.BoundaryModels.*;
@Service
@DS("pg")
public class BoundaryQueryService {
    public record Actor(long userId,String username,boolean admin) { }
    private final LandPlotMapper plots;
    private final LandPlotExcludeMapper exclusions;
    private final BoundaryAccessMapper access;
    private final WorkOrderGeoMapper geo;
    private final BoundaryClippingService clipping;
    public BoundaryQueryService(LandPlotMapper plots,LandPlotExcludeMapper exclusions,BoundaryAccessMapper access,WorkOrderGeoMapper geo,BoundaryClippingService clipping) {
        this.plots=plots;this.exclusions=exclusions;this.access=access;this.geo=geo;this.clipping=clipping;
    }
    private LandPlot load(Long plotId,Actor actor) {
        if(actor==null||actor.userId()<=0)throw new ServiceException("请先登录",401);
        if(plotId==null||plotId<=0)throw new ServiceException("请指定有效地块编号");
        if(!actor.admin()&&!access.canRead(plotId,String.valueOf(actor.userId()),actor.username()))throw new ServiceException("地块不存在或无权访问",403);
        LandPlot plot=plots.selectById(plotId);
        if(plot==null||plot.getIsDeleted()!=0)throw new ServiceException("地块不存在或无权访问",403);
        return plot;
    }
    public CheckedPoint check(Long plotId,Double longitude,Double latitude,Actor actor) {
        try {
            BoundaryCoordinates.validate(longitude,latitude);load(plotId,actor);
            String state=geo.pointState(plotId,longitude,latitude);
            Position position=state==null?Position.UNAVAILABLE:Position.valueOf(state);
            return new CheckedPoint(plotId.toString(),position,position==Position.UNAVAILABLE?null:position==Position.INSIDE||position==Position.BOUNDARY);
        } catch(IllegalArgumentException invalid){throw new ServiceException(invalid.getMessage());}
    }
    public ClippedTrack clip(Long plotId,List<Point> points,Actor actor) {
        LandPlot plot=load(plotId,actor);List<org.postgis.Polygon> holes=new ArrayList<>();
        if("插花地".equals(plot.getLandType())) {
            var query=new LambdaQueryWrapper<LandPlotExclude>().eq(LandPlotExclude::getLandPlotId,plotId).eq(LandPlotExclude::getIsDeleted,0);
            for(LandPlotExclude exclusion:exclusions.selectList(query))holes.add(exclusion.getBoundary());
        }
        try {return clipping.clip(points,plot.getBoundary(),holes);}
        catch(IllegalArgumentException invalid){throw new ServiceException(invalid.getMessage());}
    }
}
