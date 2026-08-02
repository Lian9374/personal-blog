package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.User;
import com.personalblog.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户业务
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
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
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /** 登录: 校验用户名与密码, 失败统一提示, 避免泄露账号是否存在 */
    public User login(String username, String rawPassword) {
        username = username == null ? "" : username.trim();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }

    /** 按 ID 查询用户, 不存在返回 null */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /** 修改资料: 昵称/邮箱, 新密码非空则重新加密 */
    public void updateProfile(Long id, String nickname, String email, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setNickname(nickname == null || nickname.isBlank() ? user.getUsername() : nickname.trim());
        user.setEmail(email == null ? null : email.trim());
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userMapper.updateById(user);
    }

    /** 批量查询用户(供文章/评论填充作者昵称) */
    public List<User> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids);
    }
}
