package cn.lysoy.jingu3.common.constant;

/**
 * 与 application.yml 中配置项键名一致，避免魔法字符串散落。
 * <p>注：{@code @Value} 须带默认值（如 {@code ${jingu3.user.id:001}}）；默认值含冒号时用 {@code \\:} 转义。键名与本类常量保持同步。</p>
 */
public final class Jingu3ConfigKeys {

    private Jingu3ConfigKeys() {
    }

    public static final String JINGU3_OLLAMA_BASE_URL = "jingu3.ollama.base-url";
    public static final String JINGU3_OLLAMA_CHAT_MODEL = "jingu3.ollama.chat-model";
    public static final String JINGU3_USER_ID = "jingu3.user.id";
    public static final String JINGU3_USER_NAME = "jingu3.user.name";
}
