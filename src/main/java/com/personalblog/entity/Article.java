package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章(帖子)实体
 */
@Data
@TableName("article")
public class Article {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 作者用户ID */
    private Long userId;

    /** 所属版块ID */
    private Long boardId;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 正文(Markdown 原文) */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 浏览量 */
    private Long viewCount;

    /** 评论数(去规范化) */
    private Long commentCount;

    /** 点赞数(去规范化) */
    private Long likeCount;

    /** 收藏数(去规范化) */
    private Long favoriteCount;

    /** 置顶(避免 Lombok is 前缀 getter 陷阱, 字段名不用 isPinned) */
    @TableField("is_pinned")
    private Boolean pinned;

    /** 精华 */
    @TableField("is_essence")
    private Boolean essence;

    /** 作者昵称(非表字段, 由 Service 填充展示用) */
    @TableField(exist = false)
    private String authorName;

    /** 作者头像(非表字段) */
    @TableField(exist = false)
    private String authorAvatar;

    /** 版块名(非表字段) */
    @TableField(exist = false)
    private String boardName;

    /** 标签列表(非表字段) */
    @TableField(exist = false)
    private List<Tag> tags;

    /** 当前用户是否已点赞(非表字段) */
    @TableField(exist = false)
    private Boolean liked;

    /** 当前用户是否已收藏(非表字段) */
    @TableField(exist = false)
    private Boolean favorited;

    /** Markdown 渲染后的 HTML(非表字段, 详情页展示用) */
    @TableField(exist = false)
    private String contentHtml;
}
