package com.personalblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalblog.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章 Mapper
 */
public interface ArticleMapper extends BaseMapper<Article> {

    /** 浏览量 +1 */
    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);

    /** 评论数增减(delta 可为负) */
    @Update("UPDATE article SET comment_count = comment_count + #{delta} WHERE id = #{id}")
    void changeCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /** 点赞数增减 */
    @Update("UPDATE article SET like_count = like_count + #{delta} WHERE id = #{id}")
    void changeLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 收藏数增减 */
    @Update("UPDATE article SET favorite_count = favorite_count + #{delta} WHERE id = #{id}")
    void changeFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

    /** 置顶翻转 */
    @Update("UPDATE article SET is_pinned = NOT is_pinned WHERE id = #{id}")
    void togglePinned(@Param("id") Long id);

    /** 精华翻转 */
    @Update("UPDATE article SET is_essence = NOT is_essence WHERE id = #{id}")
    void toggleEssence(@Param("id") Long id);
}
