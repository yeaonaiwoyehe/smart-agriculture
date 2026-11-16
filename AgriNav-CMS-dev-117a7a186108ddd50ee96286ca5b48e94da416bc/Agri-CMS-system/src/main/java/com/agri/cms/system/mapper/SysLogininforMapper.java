package com.agri.cms.system.mapper;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

import com.agri.cms.system.domain.SysLogininfor;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统访问日志情况信息 数据层
 *
 * @author ruoyi
 */
public interface SysLogininforMapper {
    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    public void insertLogininfor(SysLogininfor logininfor);

    /**
     * 查询系统登录日志集合
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor);

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统登录日志
     *
     * @return 结果
     */
    public int cleanLogininfor();

    @Select("select count(*) from sys_logininfor where DATE(login_time) = CURDATE()")
    Long getLoginCount();

    @Select("select count(*) from sys_logininfor where login_time between ${startTime} and ${endTime}")
    Long getLoginCount(LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT * FROM sys_logininfor WHERE user_name = #{userName} AND status = '1'")
    List<SysLogininfor> selectLoginRecordsByUserName(@Param("userName") String userName);

    @Select("SELECT COUNT(*) FROM sys_logininfor WHERE user_name = #{userName} AND status = '1'")
    int countLoginFailures(@Param("userName") String userName);

    @Select("SELECT * FROM sys_logininfor WHERE login_time BETWEEN #{startTime} AND #{endTime}")
    List<SysLogininfor> selectLoginRecordsInTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("SELECT user_name FROM sys_user WHERE status != 2")
    List<String> getAllUserNames();


    /**
     * 分页查询指定时间范围内的登录记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param offset 偏移量
     * @param limit 每页数量
     * @return List<SysLogininfor>
     * @author 李鸿麟
     * @since 2024/12/1     */
    @Select("    SELECT * " +
            "    FROM sys_logininfor " +
            "    WHERE login_time BETWEEN #{startTime} AND #{endTime} " +
            "    ORDER BY login_time DESC " +
            "    LIMIT #{offset}, #{limit} ")
    List<SysLogininfor> selectLoginRecordsWithPagination(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("offset") int offset,
            @Param("limit") int limit
    );


    /**
     * 统计指定时间范围内的记录总数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return long
     * @author 李鸿麟
     * @since 2024/12/1     */
    @Select(" SELECT COUNT(*) " +
            "    FROM sys_logininfor " +
            "    WHERE login_time BETWEEN #{startTime} AND #{endTime}")
    long countLoginRecordsInTimeRange(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );
}
