package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.User;
import com.dms.dto.UserRoleDTO;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT r.role_code FROM sys_roles r INNER JOIN sys_user_roles ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} LIMIT 1")
    String getUserRoleCode(Long userId);

    @Insert("INSERT INTO sys_user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_roles WHERE user_id = #{userId}")
    void deleteUserRole(Long userId);

    @Insert("INSERT INTO biz_students (user_id, real_name, id_card, phone, status) VALUES (#{userId}, #{realName}, #{idCard}, #{phone}, 0)")
    void insertBizStudent(@Param("userId") Long userId, @Param("realName") String realName, @Param("idCard") String idCard, @Param("phone") String phone);

    @Select("SELECT COUNT(*) FROM biz_instructors WHERE user_id = #{userId}")
    int checkBizInstructorExists(Long userId);

    @Insert("INSERT INTO biz_instructors (user_id, real_name, phone, current_load) VALUES (#{userId}, #{realName}, #{phone}, 0)")
    void insertBizInstructor(@Param("userId") Long userId, @Param("realName") String realName, @Param("phone") String phone);

    @Select("SELECT u.id, u.username, u.phone, u.status, u.created_at as createdAt, " +
            "r.id as roleId, r.role_name as roleName, r.role_code as roleCode " +
            "FROM sys_users u " +
            "LEFT JOIN sys_user_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN sys_roles r ON ur.role_id = r.id " +
            "ORDER BY u.id DESC")
    List<UserRoleDTO> getUserListWithRoles();
}
