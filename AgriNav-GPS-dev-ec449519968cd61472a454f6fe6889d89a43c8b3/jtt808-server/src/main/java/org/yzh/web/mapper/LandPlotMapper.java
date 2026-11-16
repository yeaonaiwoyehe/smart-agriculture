package org.yzh.web.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yzh.web.model.entity.LandPlot;
import java.util.List;
import java.util.Map;
@Mapper
public interface LandPlotMapper extends BaseMapper<LandPlot> {
}