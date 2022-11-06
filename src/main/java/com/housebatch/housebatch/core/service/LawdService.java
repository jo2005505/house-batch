package com.housebatch.housebatch.core.service;

import com.housebatch.housebatch.core.entity.Lawd;
import com.housebatch.housebatch.core.repository.LawdRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class LawdService {

    private final LawdRepository lawdRepository;

    @Transactional
    public void upsert (Lawd lawd) {
        // 데이터가 존재하면 Update, 존재하지 않으면 Insert
        Lawd saved = lawdRepository.findByLawdCd(lawd.getLawdCd())
                .orElseGet(Lawd::new);
        saved.setLawdCd(lawd.getLawdCd());
        saved.setLawdDong(lawd.getLawdDong());
        saved.setExist(lawd.getExist());
        lawdRepository.save(saved);
    }
}
