package cn.lysoy.jingu3.skill.mapper;

import cn.lysoy.jingu3.skill.entity.SkillEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SkillMapper extends BaseMapper<SkillEntity> {

    @Select(
            "SELECT * FROM skill WHERE is_public = TRUE AND status = 'ACTIVE' ORDER BY updated_at DESC LIMIT #{limit}")
    List<SkillEntity> selectPublicActive(@Param("limit") int limit);
}
