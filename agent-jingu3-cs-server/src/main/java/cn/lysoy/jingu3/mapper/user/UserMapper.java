package cn.lysoy.jingu3.mapper.user;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 最小用户表探测（与 Flyway V6 {@code users} 对齐）。
 */
public interface UserMapper {

    @Select("SELECT COUNT(*) FROM users WHERE id = #{id}")
    int countById(@Param("id") String id);
}
