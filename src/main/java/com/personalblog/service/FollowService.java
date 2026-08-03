package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Follow;
import com.personalblog.mapper.FollowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 关注关系
 */
@Service
public class FollowService {

    private final FollowMapper followMapper;
    private final NotificationService notificationService;

    public FollowService(FollowMapper followMapper, NotificationService notificationService) {
        this.followMapper = followMapper;
        this.notificationService = notificationService;
    }

    /** 切换关注(follower 关注 following), 返回新状态: true=已关注 */
    public boolean toggle(Long followingId, Long followerId) {
        if (followingId == null || followerId == null || followingId.equals(followerId)) {
            throw new BusinessException("不能关注自己");
        }
        Follow exist = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId));
        if (exist != null) {
            followMapper.deleteById(exist.getId());
            return false;
        }
        Follow f = new Follow();
        f.setFollowerId(followerId);
        f.setFollowingId(followingId);
        f.setCreateTime(LocalDateTime.now());
        followMapper.insert(f);
        // 仅关注建立时通知对方
        notificationService.notify(followingId, followerId, "FOLLOW", null, null);
        return true;
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId)) > 0;
    }

    /** 粉丝数 */
    public long countFollowers(Long userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, userId));
    }

    /** 关注数 */
    public long countFollowing(Long userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId));
    }

    /** 我关注了哪些人(用户主页"我的关注"等) */
    public List<Long> listFollowingIds(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId))
                .stream().map(Follow::getFollowingId).collect(Collectors.toList());
    }
}
