package com.housebatch.housebatch.job.zigbang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.housebatch.housebatch.adapter.ZigBangAptIdApiResource;

import com.housebatch.housebatch.adapter.ZigBangAptInfoApiResource;
import com.housebatch.housebatch.core.dto.ZigBangDto;
import com.housebatch.housebatch.core.entity.Apt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.housebatch.housebatch.adapter.ZigBangAptIdApiResource.*;

@Slf4j
@RequiredArgsConstructor
public class AptAndZigBangInfoMapptingProcessor implements ItemProcessor<Apt, ZigBangDto> {

    private final ZigBangAptIdApiResource zigBangAptIdApiResource;
    private final ZigBangAptInfoApiResource zigBangAptInfoApiResource;

    @Override
    public ZigBangDto process(Apt item) throws Exception {
        Long aptId = zigBangAptIdApi(item);
        return ZigBangAptInfoApi(item, aptId);
    }

    private Long zigBangAptIdApi(Apt item) throws Exception {
        BufferedReader bufferZigBangAptId = new BufferedReader(
                new InputStreamReader(zigBangAptIdApiResource.getResource(item.getAptName(), APT).getInputStream())
        );

        String line;
        StringBuffer zigBangAptId = new StringBuffer();
        while((line = bufferZigBangAptId.readLine()) != null) {
            zigBangAptId.append(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(zigBangAptId.toString());

        Long aptId = null;
        for (int idx=0;idx<jsonNode.get("items").size();idx++) {
            JsonNode tmp = jsonNode.get("items").get(idx);

            if(tmp.get("_source").has("법정동코드") &&
                    tmp.get("_source").get("법정동코드").asText().startsWith(item.getGuLawdCd())
            ) {
                System.out.println(tmp.toString());
                aptId = tmp.get("id").asLong();
                return aptId;
            } else {
                continue;
            }
        }
        return null;
    }

    private ZigBangDto ZigBangAptInfoApi(Apt item, Long zigBangAptId) throws IOException {

        if (zigBangAptId == null || !zigBangAptInfoApiResource.getResource(zigBangAptId).isReadable()) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(zigBangAptInfoApiResource.getResource(zigBangAptId).getInputStream());

        return ZigBangDto.builder()
                .aptId(item.getAptId())
                .zigBangId(jsonNode.get("id").asLong())
                .aptName(jsonNode.get("name").asText())
                .guLawdCd(item.getGuLawdCd())
                .oldAddress(nullCheckAndTransStr(jsonNode, "구주소"))
                .newAddress(nullCheckAndTransStr(jsonNode, "roadAddress"))
                .imgPath(nullCheckAndTransStr(jsonNode, "image"))
                .description(nullCheckAndTransStr(jsonNode, "desc"))
                .build();
    }

    private String nullCheckAndTransStr(JsonNode jsonNode, String key) {
        if (jsonNode.get(key).asText().equals("null")) {
            return "";
        } else if("image".equals(key)) {
            return jsonNode.get(key).asText() + "?w=500&h=375&q=60&a=1";
        } else {
            return jsonNode.get(key).asText();
        }
    }
}
