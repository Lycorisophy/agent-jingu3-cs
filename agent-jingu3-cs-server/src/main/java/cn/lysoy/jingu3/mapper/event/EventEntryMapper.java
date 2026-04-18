package cn.lysoy.jingu3.mapper.event;

import cn.lysoy.jingu3.rag.entity.EventEntryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EventEntryMapper extends BaseMapper<EventEntryEntity> {

    @Select({
        "<script>",
        "SELECT * FROM event_entry WHERE user_id = #{userId} AND id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<EventEntryEntity> selectByIdsForUser(@Param("userId") String userId, @Param("ids") List<Long> ids);
}
