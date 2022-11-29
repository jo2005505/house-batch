package com.housebatch.housebatch.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
public class ZigBangDto {
    private Long aptId;     // 아파트 ID
    private Long zigBangId; // 직방 아파트 ID
    private String aptName;      // 아파트 명
    private String guLawdCd;   // 아파트 번지
    private String oldAddress;  // 아파트 구주소
    private String newAddress;  // 아파트 신주소
    private String imgPath;     // 아파트 이미지 url
    private String description; // 아파트 설명
}
