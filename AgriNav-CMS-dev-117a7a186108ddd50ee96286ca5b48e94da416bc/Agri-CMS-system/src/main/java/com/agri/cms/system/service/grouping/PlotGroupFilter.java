package com.agri.cms.system.service.grouping;

import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.LandPlotQueryDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

public final class PlotGroupFilter {
    private PlotGroupFilter() { }
    public static void apply(LambdaQueryWrapper<LandPlot> query, LandPlotQueryDTO filter) {
        if(filter.getGroupType()==null && filter.getGroupCode()==null && !filter.isUngrouped())return;
        var type=PlotGroupType.parse(filter.getGroupType());
        if(filter.isUngrouped()) {
            if(filter.getGroupCode()!=null)throw new IllegalArgumentException("未分组条件不能同时指定分组编号");
            query.apply("NOT EXISTS (SELECT 1 FROM public.land_plot_group_assignment ga WHERE ga.land_plot_id=land_plot.id AND ga.group_type={0})",type.name());
        } else {
            String code=type.validateCode(filter.getGroupCode());
            query.apply("EXISTS (SELECT 1 FROM public.land_plot_group_assignment ga WHERE ga.land_plot_id=land_plot.id AND ga.group_type={0} AND ga.group_code={1})",type.name(),code);
        }
    }
}
