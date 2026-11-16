package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.Crop;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author 李鸿麟
 */
@DS("pg")
public interface CropMapper extends BaseMapper<Crop> {
    @Select("SELECT COUNT(*) > 0 FROM crop WHERE crop_name = #{cropName}")
    boolean checkCropIsInsertedByName(String cropName);
}
