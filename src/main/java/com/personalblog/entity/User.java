package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。注意: user 是 PostgreSQL 保留字, @TableName 必须用双引号包裹。
 */
@Data
@TableName("\"user\"")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录名(唯一) */
    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 头像(base64 data URI, 存库保证 Render 无持久磁盘也能保留) */
    private String avatar;

    /** 角色 USER / ADMIN */
    private String role;

    /** 个人简介/签名 */
    private String bio;

    /** 状态 ACTIVE / BANNED */
    private String status;

    /** 注册时间 */
    private LocalDateTime createdAt;

    // ---------- 展示字段(非表字段, 由 Service 填充) ----------

    /** 发帖数 */
    @TableField(exist = false)
    private Long postCount;

    /** 评论数 */
    @TableField(exist = false)
    private Long commentCount;

    /** 粉丝数 */
    @TableField(exist = false)
    private Long followerCount;

    /** 关注数 */
    @TableField(exist = false)
    private Long followingCount;

    /** 收藏数 */
    @TableField(exist = false)
    private Long favoriteCount;

    /** 当前用户是否已关注 TA */
    @TableField(exist = false)
    private Boolean followedByCurrentUser;
}
