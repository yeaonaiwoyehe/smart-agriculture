package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.CropWork;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author 李鸿麟
 * @description 针对作物、工作类型关联表操作的Mapper
 */
@DS("pg")
public interface CropWorkMapper extends BaseMapper<CropWork> {

    @Select("SELECT MAX(crop_id) FROM crop_work")
    Integer selectMaxCropId();

}
