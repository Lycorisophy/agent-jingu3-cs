package cn.lysoy.jingu3.mapper.workspace;

import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceExecutionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WorkspaceExecutionMapper extends BaseMapper<WorkspaceExecutionEntity> {

    @Select("SELECT * FROM workspace_execution WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<WorkspaceExecutionEntity> selectRecentByUserId(@Param("userId") String userId, @Param("limit") int limit);
}
