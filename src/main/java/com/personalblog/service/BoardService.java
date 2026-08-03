package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Board;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.BoardMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 版块
 */
@Service
public class BoardService {

    private final BoardMapper boardMapper;
    private final ArticleMapper articleMapper;

    public BoardService(BoardMapper boardMapper, ArticleMapper articleMapper) {
        this.boardMapper = boardMapper;
        this.articleMapper = articleMapper;
    }

    /** 版块总数 */
    public long count() {
        return boardMapper.selectCount(new LambdaQueryWrapper<>());
    }

    /** 全部版块(按 sort_order, id 排序) */
    public List<Board> listAll() {
        return boardMapper.selectList(new LambdaQueryWrapper<Board>()
                .orderByAsc(Board::getSortOrder)
                .orderByAsc(Board::getId));
    }

    /** 全部版块并附带文章数/最后发帖时间 */
    public List<Board> listAllWithCounts() {
        List<Board> boards = listAll();
        List<Map<String, Object>> rows = articleMapper.selectMaps(new QueryWrapper<Article>()
                .select("board_id", "COUNT(*) AS cnt", "MAX(create_time) AS last")
                .groupBy("board_id"));
        Map<Long, Long> countMap = new HashMap<>();
        Map<Long, Object> lastMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long boardId = ((Number) row.get("board_id")).longValue();
            countMap.put(boardId, ((Number) row.get("cnt")).longValue());
            lastMap.put(boardId, row.get("last"));
        }
        for (Board b : boards) {
            b.setArticleCount(countMap.getOrDefault(b.getId(), 0L));
            Object last = lastMap.get(b.getId());
            if (last instanceof java.sql.Timestamp ts) {
                b.setLastArticleTime(ts.toLocalDateTime());
            }
        }
        return boards;
    }

    /** 按 ID 查询, 不存在抛 404 */
    public Board getById(Long id) {
        Board board = boardMapper.selectById(id);
        if (board == null) {
            throw new BusinessException(404, "版块不存在");
        }
        return board;
    }

    public void create(String name, String description, Integer sortOrder) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("版块名称不能为空");
        }
        if (trimmed.length() > 50) {
            throw new BusinessException("版块名称不能超过 50 字");
        }
        Board board = new Board();
        board.setName(trimmed);
        board.setDescription(description == null ? "" : description.trim());
        board.setSortOrder(sortOrder == null ? 0 : sortOrder);
        board.setCreateTime(LocalDateTime.now());
        boardMapper.insert(board);
    }

    public void update(Long id, String name, String description, Integer sortOrder) {
        Board board = getById(id);
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("版块名称不能为空");
        }
        board.setName(trimmed);
        board.setDescription(description == null ? "" : description.trim());
        board.setSortOrder(sortOrder == null ? 0 : sortOrder);
        boardMapper.updateById(board);
    }

    /** 删除版块; 版块下仍有帖子则拒绝 */
    public void delete(Long id) {
        getById(id);
        long cnt = articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getBoardId, id));
        if (cnt > 0) {
            throw new BusinessException("该版块下还有帖子, 不能删除");
        }
        boardMapper.deleteById(id);
    }
}
