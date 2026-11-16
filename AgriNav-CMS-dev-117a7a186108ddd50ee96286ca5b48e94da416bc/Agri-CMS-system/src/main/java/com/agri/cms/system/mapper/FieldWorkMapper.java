package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.FieldWork;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

@DS("pg")
public interface FieldWorkMapper extends BaseMapper<FieldWork> {
    @Select("SELECT COUNT(*) > 0 FROM work WHERE work_name = #{workName}")
    Boolean checkWorkIsInsertedByName(String workName);


}
