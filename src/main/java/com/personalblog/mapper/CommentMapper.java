package com.personalblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalblog.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评论 Mapper
 */
public interface CommentMapper extends BaseMapper<Comment> {

    /** 计算下一楼层号 */
    @Select("SELECT COALESCE(MAX(floor), 0) + 1 FROM comment WHERE article_id = #{articleId}")
    int nextFloor(@Param("articleId") Long articleId);
}
