package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版块
 */
@Data
@TableName("board")
public class Board {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Integer sortOrder;

    /** 版块图标(emoji) */
    private String icon;

    private LocalDateTime createTime;

    /** 文章数(展示用, 由 Service 填充) */
    @TableField(exist = false)
    private Long articleCount;

    /** 最后发帖时间(展示用) */
    @TableField(exist = false)
    private LocalDateTime lastArticleTime;
}
