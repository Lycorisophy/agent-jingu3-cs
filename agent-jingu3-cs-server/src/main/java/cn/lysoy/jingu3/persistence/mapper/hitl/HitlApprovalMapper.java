package cn.lysoy.jingu3.persistence.mapper.hitl;

import cn.lysoy.jingu3.hitl.HitlApprovalStatus;
import cn.lysoy.jingu3.hitl.entity.HitlApprovalEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface HitlApprovalMapper extends BaseMapper<HitlApprovalEntity> {

    @Select(
            "SELECT * FROM hitl_approval WHERE status = #{status} AND conversation_id = #{conversationId} "
                    + "ORDER BY created_at ASC")
    List<HitlApprovalEntity> selectByStatusAndConversationIdOrderByCreatedAtAsc(
            @Param("status") HitlApprovalStatus status, @Param("conversationId") String conversationId);
}
