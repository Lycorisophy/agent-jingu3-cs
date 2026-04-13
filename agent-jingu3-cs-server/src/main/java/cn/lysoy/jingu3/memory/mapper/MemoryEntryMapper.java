package cn.lysoy.jingu3.memory.mapper;

import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MemoryEntryMapper extends BaseMapper<MemoryEntryEntity> {

    @Select(
            "SELECT * FROM memory_entry WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<MemoryEntryEntity> selectByUserIdOrderByCreatedAtDesc(
            @Param("userId") String userId, @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT * FROM memory_entry WHERE id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<MemoryEntryEntity> selectByIds(@Param("ids") List<Long> ids);
}
