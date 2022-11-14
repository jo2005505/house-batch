package com.housebatch.housebatch.core.repository;

import com.housebatch.housebatch.core.entity.Lawd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LawdRepository extends JpaRepository<Lawd, Long> {
    // 데이터베이스에 데이터가 존재하는지 확인하는 구문
    Optional<Lawd> findByLawdCd(String lawdCd);

    /**
     * 법정동 코드 추출의 고려사항
     *   1. lawd_cd의 unique한 값을 추출하기 위해서 법정동 코드 앞 5자리를 DISTINCT 문법 사용
     *   2. 특별 시/도만을 제공하는 컬럼은 동까지의 데이터가 없기 때문에 제외 (like 문으로 처리)
     *
     * SELECT
     *     DISTINCT(SUBSTRING(lawd_cd, 1, 5)) AS lawd_cd
     * FROM
     *     lawd
     * WHERE
     *     exist = 1
     *     AND lawd_cd not like '%00000000'
     * 
     * ** JPA에서 DISTINCT 쿼리와 같은 문법은 제공하지 않음
     *    - @Query Annotation을 사용 : https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query
     *    - MySQL Query를 Jpa Query로 수정이 필요
     */
    @Query("SELECT DISTINCT SUBSTRING(l.lawdCd, 1, 5) FROM Lawd l WHERE l.exist = 1 AND l.lawdCd NOT LIKE '%00000000'")
    List<String> findDistinctGuLawdCd();
}
