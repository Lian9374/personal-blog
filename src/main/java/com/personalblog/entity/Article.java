package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体
 */
@Data
@TableName("article")
public class Article {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 作者用户ID */
    private Long userId;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 正文 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 作者昵称(非表字段, 由 Service 填充展示用) */
    @TableField(exist = false)
    private String authorName;

    /** 评论数(非表字段, 由 Service 填充展示用) */
    @TableField(exist = false)
    private Long commentCount;
}
