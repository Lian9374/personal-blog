package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体。注意: `user` 是 MySQL 保留字, @TableName 必须用反引号包裹。
 */
@Data
@TableName("`user`")
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

    /** 注册时间 */
    private LocalDateTime createdAt;
}
