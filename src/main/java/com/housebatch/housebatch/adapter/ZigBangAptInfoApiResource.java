package com.housebatch.housebatch.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Slf4j
@Component
public class ZigBangAptInfoApiResource {

    @Value("${external.zigbang-api.aptInfo}")
    private String aptInfo;

    public Resource getResource(
            Long aptId
    ) {
        String url = String.format("%s/%s", aptInfo, aptId);

        log.info("ZigBangAptInfoApiResource Resource URL ::: " + url);

        try {
            return new UrlResource(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failed to Create ZigBangAptInfoApiResource UrlResource");
        }
    }
}
