package com.housebatch.housebatch.core.repository;

import com.housebatch.housebatch.core.entity.Apt;
import com.housebatch.housebatch.core.entity.AptDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AptDealRepository extends JpaRepository<AptDeal, Long> {
    /**
     * 공공데이터 포탈에서 제공하는 API에서 거래를 구분할 유니크 값이 존재하지 않기 때문에 거래 변동 시 구분하기 위한 조건 조회
     * @param apt
     * @param exclusiveArea
     * @param dealDate
     * @param dealAmount
     * @param floor
     */
    Optional<AptDeal> findAptDealByAptAndExclusiveAreaAndDealDateAndDealAmountAndFloor(
            Apt apt
            , Double exclusiveArea
            , LocalDate dealDate
            , Long dealAmount
            , Integer floor
    );

    /**
     * 원하는 거래 일자의 데이터 조회
     * @param localDate
     */
    @Query("select  ad from AptDeal ad join fetch ad.apt where ad.dealCanceled = 0 and ad.dealDate = ?1")
    List<AptDeal> findByDealCanceledIsFalseAndDealDateEquals(LocalDate localDate);
}
