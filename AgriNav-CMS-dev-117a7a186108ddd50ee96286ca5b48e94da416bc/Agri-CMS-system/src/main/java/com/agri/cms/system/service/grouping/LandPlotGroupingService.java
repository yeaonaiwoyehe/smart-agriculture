package com.agri.cms.system.service.grouping;

import com.agri.cms.system.mapper.LandPlotGroupingMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@DS("pg")
public class LandPlotGroupingService {
    private final LandPlotGroupingMapper mapper;
    public LandPlotGroupingService(LandPlotGroupingMapper mapper) { this.mapper=mapper; }

    public Map<String,Object> summary(String type,long userId) {
        var groupType=PlotGroupType.parse(type); String owner=owner(userId);
        boolean ready=mapper.isReady();
        return Map.of("available",ready,"groups",ready?mapper.summary(groupType.name(),owner):List.of());
    }

    public List<Map<String,Object>> assignments(List<String> ids,long userId) {
        String owner=owner(userId);var plots=ids(ids);
        return mapper.isReady()?mapper.assignments(plots,owner):List.of();
    }

    @Transactional(rollbackFor=Exception.class)
    public void create(String type,String code,String name,long userId) {
        var groupType=PlotGroupType.parse(type);String normalizedCode=groupType.validateCode(code);
        String normalizedName=PlotGroupType.validateName(name),owner=owner(userId);requireReady();
        mapper.createGroup(groupType.name(),normalizedCode,normalizedName,owner);
        if(!normalizedName.equals(mapper.groupName(groupType.name(),normalizedCode)))
            throw new IllegalArgumentException("该编号已对应其他名称，请核对后使用已有分组");
    }

    /** 请求中的用户身份不能覆盖登录身份；整批锁定并确认归属后才写关联表。 */
    @Transactional(rollbackFor=Exception.class)
    public void assign(String type,String code,List<String> ids,long userId) {
        var groupType=PlotGroupType.parse(type);String owner=owner(userId);
        var plots=ids(ids);String groupCode=code==null?null:groupType.validateCode(code);requireReady();
        if(groupCode!=null&&mapper.groupName(groupType.name(),groupCode)==null)
            throw new IllegalArgumentException("目标分组不存在，请先创建分组");
        if(!new HashSet<>(mapper.lockOwnedPlots(plots,owner)).equals(new HashSet<>(plots)))
            throw new IllegalArgumentException("部分地块不存在、已删除或不属于当前用户，整批未修改");
        if(groupCode==null) mapper.unassign(plots,groupType.name());
        else if(mapper.assign(plots,groupType.name(),groupCode,owner)!=plots.size())
            throw new IllegalStateException("归组数量不一致，整批修改已回滚");
    }

    private void requireReady() {
        if(!mapper.isReady()) throw new IllegalArgumentException("分组功能尚未初始化，请应用 database/land_plot_grouping_ddl.sql 后重试");
    }
    private String owner(long userId) {
        if(userId<=0) throw new IllegalArgumentException("登录身份无效");return String.valueOf(userId);
    }
    private List<Long> ids(List<String> values) {
        if(values==null||values.isEmpty()||values.size()>1000)throw new IllegalArgumentException("每次请选择1至1000块地");
        List<Long> result=new ArrayList<>();
        for(String value:values) {
            try {if(value==null||!value.matches("[1-9][0-9]{0,18}"))throw new NumberFormatException();result.add(Long.parseLong(value));}
            catch(NumberFormatException invalid){throw new IllegalArgumentException("地块编号格式无效");}
        }
        if(new HashSet<>(result).size()!=result.size())throw new IllegalArgumentException("请勿重复提交同一地块");
        return result;
    }
}
