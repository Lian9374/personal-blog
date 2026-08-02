package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
@TableName("comment")
public class Comment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属文章ID */
    private Long articleId;

    /** 评论用户ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 评论时间 */
    private LocalDateTime createTime;

    /** 评论人昵称(非表字段, 由 Service 填充展示用) */
    @TableField(exist = false)
    private String authorName;
}
