package cn.lysoy.jingu3.mapper.event;

import cn.lysoy.jingu3.rag.entity.EventRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EventRelationMapper extends BaseMapper<EventRelationEntity> {

    /**
     * 任一端落在给定 id 集合中的关系（用户隔离）。
     */
    @Select({
        "<script>",
        "SELECT * FROM event_relation WHERE user_id = #{userId}",
        "AND (event_a_id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "OR event_b_id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        ")",
        "</script>"
    })
    List<EventRelationEntity> findTouching(@Param("userId") String userId, @Param("ids") List<Long> ids);
}
