package com.housebatch.housebatch.core.entity;

import com.housebatch.housebatch.core.dto.AptDealDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "apt_deal")
@EntityListeners(AuditingEntityListener.class)
public class AptDeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aptDealId;
    
    /*
     * 다수의 아파트 매매가 하나의 아파트와 맵핑됨을 의미
     */
    @ManyToOne
    @JoinColumn(name = "apt_id")
    private Apt apt;

    @Column(nullable = false)
    private Double exclusiveArea;

    @Column(nullable = false)
    private LocalDate dealDate;

    @Column(nullable = false)
    private Long dealAmount;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private boolean dealCanceled;

    @Column(nullable = true)
    private LocalDate dealCanceledDate;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static AptDeal of(AptDealDto dto, Apt apt) {
        return AptDeal.builder()
                .apt(apt)
                .exclusiveArea(dto.getExclusiveArea())
                .dealDate(dto.getDealDate())
                .dealAmount(dto.getDealAmount())
                .floor(dto.getFloor())
                .dealCanceled(dto.isDealCanceled())
                .dealCanceledDate(dto.getDealCanceledDate())
                .build();
    }
}
