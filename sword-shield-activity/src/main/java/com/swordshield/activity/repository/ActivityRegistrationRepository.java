package com.swordshield.activity.repository;

import com.swordshield.activity.domain.ActivityRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {
    Optional<ActivityRegistration> findByActivityIdAndUserId(Long activityId, Long userId);
    long countByActivityId(Long activityId);
    List<ActivityRegistration> findByUserId(Long userId);
    Page<ActivityRegistration> findByUserId(Long userId, Pageable pageable);

    /** 渠道溯源：按报名来源分组统计。 */
    @Query("select r.channel, count(r) from ActivityRegistration r where r.activityId = :activityId group by r.channel")
    List<Object[]> countByChannel(@Param("activityId") Long activityId);
}

