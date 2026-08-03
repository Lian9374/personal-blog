package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.ArticleTag;
import com.personalblog.entity.Tag;
import com.personalblog.mapper.ArticleTagMapper;
import com.personalblog.mapper.TagMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 标签
 */
@Service
public class TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    public TagService(TagMapper tagMapper, ArticleTagMapper articleTagMapper) {
        this.tagMapper = tagMapper;
        this.articleTagMapper = articleTagMapper;
    }

    /** 按名称查询(不存在返回 null) */
    public Tag getByName(String name) {
        return tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
    }

    /** 按名称查找, 不存在则创建(并发下捕获唯一索引冲突后重查) */
    public Tag findOrCreate(String name) {
        String trimmed = name.trim();
        Tag tag = getByName(trimmed);
        if (tag != null) {
            return tag;
        }
        tag = new Tag();
        tag.setName(trimmed);
        tag.setCreateTime(LocalDateTime.now());
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            tag = getByName(trimmed);
        }
        return tag;
    }

    /** 某文章的标签列表 */
    public List<Tag> listByArticle(Long articleId) {
        List<ArticleTag> links = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = links.stream().map(ArticleTag::getTagId).toList();
        return tagMapper.selectBatchIds(tagIds);
    }

    /** 批量查询多篇文章的标签: articleId -> List<Tag> */
    public java.util.Map<Long, List<Tag>> listByArticleIds(java.util.Collection<Long> articleIds) {
        java.util.Map<Long, List<Tag>> result = new java.util.HashMap<>();
        if (articleIds == null || articleIds.isEmpty()) {
            return result;
        }
        List<ArticleTag> links = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        if (links.isEmpty()) {
            return result;
        }
        List<Long> tagIds = links.stream().map(ArticleTag::getTagId).distinct().toList();
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        java.util.Map<Long, Tag> tagMap = new java.util.HashMap<>();
        tags.forEach(t -> tagMap.put(t.getId(), t));
        for (ArticleTag link : links) {
            result.computeIfAbsent(link.getArticleId(), k -> new ArrayList<>())
                    .add(tagMap.get(link.getTagId()));
        }
        return result;
    }

    /** 保存文章的标签关联: 与现有集合 diff, 删多插少 */
    public void saveArticleTags(Long articleId, List<String> names) {
        Set<String> newNames = new HashSet<>();
        if (names != null) {
            for (String n : names) {
                if (n != null && !n.isBlank()) {
                    newNames.add(n.trim());
                }
            }
        }
        List<ArticleTag> oldLinks = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        Set<Long> oldTagIds = new HashSet<>();
        for (ArticleTag link : oldLinks) {
            oldTagIds.add(link.getTagId());
        }
        Set<Long> newTagIds = new HashSet<>();
        for (String n : newNames) {
            newTagIds.add(findOrCreate(n).getId());
        }
        // 删除被移除的
        for (ArticleTag link : oldLinks) {
            if (!newTagIds.contains(link.getTagId())) {
                articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getArticleId, articleId)
                        .eq(ArticleTag::getTagId, link.getTagId()));
            }
        }
        // 插入新增的
        for (Long tagId : newTagIds) {
            if (!oldTagIds.contains(tagId)) {
                ArticleTag link = new ArticleTag();
                link.setArticleId(articleId);
                link.setTagId(tagId);
                articleTagMapper.insert(link);
            }
        }
    }

    /** 按名称取标签, 不存在抛 404 */
    public Tag getByNameOr404(String name) {
        Tag tag = getByName(name);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return tag;
    }
}
