package org.yzh.web.trajectory.service.Impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.postgis.Polygon;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yzh.web.mapper.LandPlotMapper;
import org.yzh.web.model.entity.LandPlot;
import org.yzh.web.model.vo.LandPlotVO;
import org.yzh.web.trajectory.service.LandPlotService;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author simon
 * @since 2024/9/19 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LandPlotServiceImpl extends ServiceImpl<LandPlotMapper, LandPlot> implements LandPlotService {
}