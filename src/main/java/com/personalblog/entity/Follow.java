package com.personalblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系 (follower 关注 following)
 */
@Data
@TableName("follow")
public class Follow {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关注者 */
    private Long followerId;

    /** 被关注者 */
    private Long followingId;

    private LocalDateTime createTime;
}
