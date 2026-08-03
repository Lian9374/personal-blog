package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Comment;
import com.personalblog.entity.Favorite;
import com.personalblog.entity.Follow;
import com.personalblog.entity.User;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.CommentMapper;
import com.personalblog.mapper.FavoriteMapper;
import com.personalblog.mapper.FollowMapper;
import com.personalblog.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户业务
 */
@Service
public class UserService {

    private static final long MAX_AVATAR_BYTES = 1_048_576; // 1MB

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final FollowMapper followMapper;
    private final FavoriteMapper favoriteMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, ArticleMapper articleMapper, CommentMapper commentMapper,
                       FollowMapper followMapper, FavoriteMapper favoriteMapper) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.followMapper = followMapper;
        this.favoriteMapper = favoriteMapper;
    }

    /** 用户名是否已存在 */
    public boolean usernameExists(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    /** 注册: 查重 -> BCrypt 加密 -> 入库 */
    public User register(String username, String rawPassword, String nickname, String email) {
        username = username == null ? "" : username.trim();
        if (username.isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (usernameExists(username)) {
            throw new BusinessException("用户名已存在");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname.trim());
        user.setEmail(email == null ? null : email.trim());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setBio("");
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /** 登录: 校验用户名/密码/账号状态, 失败统一提示 */
    public User login(String username, String rawPassword) {
        username = username == null ? "" : username.trim();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被封禁，请联系管理员");
        }
        return user;
    }

    /** 按 ID 查询用户, 不存在返回 null */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /** 批量查询用户(供文章/评论填充作者信息) */
    public List<User> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids);
    }

    /** 修改资料: 昵称/邮箱/简介, 新密码非空则重新加密 */
    public void updateProfile(Long id, String nickname, String email, String bio, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setNickname(nickname == null || nickname.isBlank() ? user.getUsername() : nickname.trim());
        user.setEmail(email == null ? null : email.trim());
        user.setBio(bio == null ? "" : bio.trim());
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userMapper.updateById(user);
    }

    /** 更新头像: 校验魔数与大小后以 base64 data URI 存库(Render 无持久磁盘, 不落盘) */
    public void updateAvatar(Long id, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (bytes.length > MAX_AVATAR_BYTES) {
            throw new BusinessException("图片不能超过 1MB");
        }
        String mime = detectImageType(bytes);
        if (mime == null) {
            throw new BusinessException("仅支持 JPG/PNG/GIF/WEBP 图片");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        user.setAvatar("data:" + mime + ";base64," + base64);
        userMapper.updateById(user);
    }

    /** 魔数探测图片类型: jpeg/png/gif/webp, 不信任 Content-Type */
    private String detectImageType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return "image/gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    /** 个人主页资料 + 各项统计 */
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPostCount(articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getUserId, userId)));
        user.setCommentCount(commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId)));
        user.setFollowerCount(followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, userId)));
        user.setFollowingCount(followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId)));
        user.setFavoriteCount(favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)));
        return user;
    }

    /** 用户总数 */
    public long count() {
        return userMapper.selectCount(new LambdaQueryWrapper<>());
    }

    /** 活跃成员(右栏发现): 按发帖数取 Top n */
    public List<User> topUsers(int n) {
        List<Map<String, Object>> rows = articleMapper.selectMaps(new QueryWrapper<Article>()
                .select("user_id", "COUNT(*) AS cnt")
                .groupBy("user_id")
                .orderByDesc("cnt")
                .last("LIMIT " + n));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> cntMap = new java.util.HashMap<>();
        List<Long> ids = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            ids.add(userId);
            cntMap.put(userId, ((Number) row.get("cnt")).longValue());
        }
        return userMapper.selectBatchIds(ids).stream()
                .peek(u -> u.setPassword(null))
                .peek(u -> u.setPostCount(cntMap.getOrDefault(u.getId(), 0L)))
                .toList();
    }

    /** 最近注册的 N 个用户(管理端仪表盘) */
    public List<User> latestUsers(int n) {
        return userMapper.selectPage(new Page<>(1, n),
                        new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt))
                .getRecords().stream().peek(u -> u.setPassword(null)).toList();
    }

    /** 搜索用户(用户名/昵称), 最多 20 条 */
    public List<User> searchUser(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return List.of();
        }
        return userMapper.selectPage(new Page<>(1, 20),
                        new LambdaQueryWrapper<User>()
                                .and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                                .orderByAsc(User::getId))
                .getRecords().stream().peek(u -> u.setPassword(null)).toList();
    }

    // ==================== 管理端 ====================

    public void updateRole(Long id, String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException("非法角色");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    public void updateStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"BANNED".equals(status)) {
            throw new BusinessException("非法状态");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public IPage<User> pageUsers(long current, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .and(!kw.isEmpty(), w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .orderByDesc(User::getCreatedAt);
        IPage<User> page = userMapper.selectPage(new Page<>(Math.max(current, 1), 20), qw);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }
}
