package cn.lysoy.jingu3.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("user_prompt_cipher")
@Getter
@Setter
public class UserPromptCipherEntity {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("ciphertext_b64")
    private String ciphertextB64;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
