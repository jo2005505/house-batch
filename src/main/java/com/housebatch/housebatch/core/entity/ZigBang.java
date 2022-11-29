package com.housebatch.housebatch.core.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Table(name = "zigbang")
public class ZigBang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(nullable = false)
    private Long aptId;

    @Column(nullable = false)
    private Long zigBangId;

    @Column(nullable = false)
    private String aptName;

    @Column(nullable = false)
    private String guLawdCd;

    @Column(nullable = true)
    private String oldAddress;

    @Column(nullable = true)
    private String newAddress;

    @Column(nullable = true)
    private String imgPath;

    @Column(nullable = true)
    private String description;
}
