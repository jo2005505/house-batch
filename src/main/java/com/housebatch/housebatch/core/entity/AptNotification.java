package com.housebatch.housebatch.core.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "apt_notification")
@EntityListeners(AutoCloseable.class)
public class AptNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aptNotificationId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String guLawdCd;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
