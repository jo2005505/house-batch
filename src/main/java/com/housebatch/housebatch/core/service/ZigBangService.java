package com.housebatch.housebatch.core.service;

import com.housebatch.housebatch.core.dto.ZigBangDto;
import com.housebatch.housebatch.core.entity.ZigBang;
import com.housebatch.housebatch.core.repository.ZigBangRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ZigBangService {

    private final ZigBangRepository zigBangRepository;

    @Transactional
    public void upsert (ZigBangDto zigBangDto) {
        if (zigBangDto == null) {
            return;
        }
        ZigBang saved = zigBangRepository.findByZigBangId(zigBangDto.getZigBangId())
                .orElseGet(ZigBang::new);
        saved.setAptId(zigBangDto.getAptId());
        saved.setZigBangId(zigBangDto.getZigBangId());
        saved.setAptName(zigBangDto.getAptName());
        saved.setGuLawdCd(zigBangDto.getGuLawdCd());
        saved.setOldAddress(zigBangDto.getOldAddress());
        saved.setNewAddress(zigBangDto.getNewAddress());
        saved.setImgPath(zigBangDto.getImgPath());
        saved.setDescription(zigBangDto.getDescription());
        zigBangRepository.save(saved);
    }
}
