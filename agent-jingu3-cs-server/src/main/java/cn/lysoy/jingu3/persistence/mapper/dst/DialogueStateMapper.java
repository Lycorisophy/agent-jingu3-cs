package cn.lysoy.jingu3.persistence.mapper.dst;

import cn.lysoy.jingu3.dst.entity.DialogueStateEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DialogueStateMapper extends BaseMapper<DialogueStateEntity> {

    @Select("SELECT * FROM dialogue_state WHERE conversation_id = #{conversationId} LIMIT 1")
    DialogueStateEntity selectByConversationId(@Param("conversationId") String conversationId);
}
