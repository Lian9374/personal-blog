package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 接收者 */
    private Long recipientId;

    /** 触发者 */
    private Long actorId;

    /** REPLY / LIKE / FAVORITE / FOLLOW */
    private String type;

    private Long articleId;

    private Long commentId;

    private Boolean isRead;

    private LocalDateTime createTime;

    /** 展示用: 触发者昵称/头像/相关文章标题/类型中文 */
    @TableField(exist = false)
    private String actorName;

    @TableField(exist = false)
    private String actorAvatar;

    @TableField(exist = false)
    private String articleTitle;

    @TableField(exist = false)
    private String typeText;
}
