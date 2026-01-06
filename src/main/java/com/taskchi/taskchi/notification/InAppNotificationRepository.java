package com.taskchi.taskchi.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    List<InAppNotification> findTop50ByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);
    List<InAppNotification> findTop200ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        select n from InAppNotification n
        left join fetch n.user u
        where n.id = :id
    """)
    Optional<InAppNotification> findByIdWithUser(@Param("id") Long id);

}
