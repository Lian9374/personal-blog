package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章-标签 关联(代理主键)
 */
@Data
@TableName("article_tag")
public class ArticleTag {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long tagId;
}
