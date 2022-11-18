package com.housebatch.housebatch.core.repository;

import com.housebatch.housebatch.core.entity.Apt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AptRepository extends JpaRepository<Apt, Long> {
    Optional<Apt> findAptByAptNameAndJibun(String aptName, String jibun);
}
