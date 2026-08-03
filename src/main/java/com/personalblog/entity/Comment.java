package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 回复的父评论ID(NULL 表示顶层评论) */
    private Long parentId;

    /** 楼层(同文章内顶层评论按序编号) */
    private Integer floor;

    /** 评论内容 */
    private String content;

    /** 评论时间 */
    private LocalDateTime createTime;

    /** 评论人昵称(非表字段, 由 Service 填充展示用) */
    @TableField(exist = false)
    private String authorName;

    /** 评论人头像(非表字段) */
    @TableField(exist = false)
    private String authorAvatar;

    /** 回复目标的昵称(非表字段, 展示"回复 @xx") */
    @TableField(exist = false)
    private String replyToName;

    /** 回复树(非表字段, 顶层评论的 children) */
    @TableField(exist = false)
    private List<Comment> children;
}
