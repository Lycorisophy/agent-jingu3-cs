package cn.lysoy.jingu3.mapper.workspace;

import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WorkspaceMapper extends BaseMapper<WorkspaceEntity> {

    @Select("SELECT * FROM workspace WHERE user_id = #{userId}")
    WorkspaceEntity selectByUserId(@Param("userId") String userId);
}
