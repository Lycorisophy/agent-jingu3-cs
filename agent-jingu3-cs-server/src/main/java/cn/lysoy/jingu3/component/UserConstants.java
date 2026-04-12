package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.constant.Jingu3ConfigKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用户系统史诗完成前的单用户种子（与开发规范一致）。
 * <p>配置键见 {@link Jingu3ConfigKeys#JINGU3_USER_ID}、{@link Jingu3ConfigKeys#JINGU3_USER_NAME}。</p>
 */
@Component
public class UserConstants {

    private final String id;
    private final String username;

    public UserConstants(
            @Value("${jingu3.user.id:001}") String id,
            @Value("${jingu3.user.name:user}") String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
