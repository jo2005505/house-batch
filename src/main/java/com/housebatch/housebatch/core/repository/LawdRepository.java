package com.housebatch.housebatch.core.repository;

import com.housebatch.housebatch.core.entity.Lawd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LawdRepository extends JpaRepository<Lawd, Long> {
    // 데이터베이스에 데이터가 존재하는지 확인하는 구문
    Optional<Lawd> findByLawdCd(String lawdCd);
}
