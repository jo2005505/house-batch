package com.housebatch.housebatch.core.service;

import com.housebatch.housebatch.core.dto.AptDealDto;
import com.housebatch.housebatch.core.entity.Apt;
import com.housebatch.housebatch.core.entity.AptDeal;
import com.housebatch.housebatch.core.repository.AptDealRepository;
import com.housebatch.housebatch.core.repository.AptRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AptDealDto에 있는 값을 Apt, AptDeal Entity로 저장
 */
@Service
@AllArgsConstructor
public class AptDealService {
    /**
     * Effective Java Guid
     * 1. 하나의 파라미터를 받는 경우 from 으로 메소드명 작성
     * 2. 다수의 파라미터를 받는 경우 of 로 메소드명 작성
     */
    private final AptRepository aptRepository;
    private final AptDealRepository aptDealRepository;

    @Transactional
    public void upsert(AptDealDto dto) {
        Apt apt = getAptOrNew(dto);
        saveAptDeal(dto, apt);
    }

    private Apt getAptOrNew(AptDealDto dto) {
        Apt apt = aptRepository.findAptByAptNameAndJibun(dto.getAptName(), dto.getJibun())
                .orElseGet(() -> Apt.from(dto));
        return aptRepository.save(apt);
    }

    private void saveAptDeal(AptDealDto dto, Apt apt) {
        AptDeal aptDeal = aptDealRepository.findAptDealByAptAndExclusiveAreaAndDealDateAndDealAmountAndFloor(
                apt
                , dto.getExclusiveArea()
                , dto.getDealDate()
                , dto.getDealAmount()
                , dto.getFloor()
        ).orElseGet(() -> AptDeal.of(dto, apt));
        aptDeal.setDealCanceled(dto.isDealCanceled());
        aptDeal.setDealCanceledDate(dto.getDealCanceledDate());
        aptDealRepository.save(aptDeal);
    }
}
