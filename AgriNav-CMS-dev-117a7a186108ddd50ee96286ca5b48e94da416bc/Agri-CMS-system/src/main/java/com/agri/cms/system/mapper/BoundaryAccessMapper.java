package com.agri.cms.system.mapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;
@Mapper
@DS("pg")
public interface BoundaryAccessMapper {
    @Select("""
        SELECT EXISTS(SELECT 1 FROM public.land_plot p WHERE p.id=#{plotId} AND p.is_deleted=0 AND
          (p.create_by=#{userId} OR EXISTS(SELECT 1 FROM public.trajectory_info t WHERE t.plot_id=p.id
          AND (t.status IS NULL OR t.status<>2) AND (t.created_by=#{userId} OR t.phone_number=#{username}))))
        """)
    boolean canRead(@Param("plotId") Long plotId,@Param("userId") String userId,@Param("username") String username);
}
