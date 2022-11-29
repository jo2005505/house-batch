package com.housebatch.housebatch.core.repository;

import com.housebatch.housebatch.core.entity.ZigBang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZigBangRepository extends JpaRepository<ZigBang, Long> {
    Optional<ZigBang> findByZigBangId(Long zigBangId);
}
