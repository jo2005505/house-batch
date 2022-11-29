package com.housebatch.housebatch.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ZigBangAptIdApiResource {

    @Value("${external.zigbang-api.search}")
    private String searchPath;

    public static String APT = "아파트";

    public Resource getResource(
            String aptName, String serviceType
    ) {
        String url = String.format("%s?leaseYn=N&q=%s&serviceType=%s",
                searchPath, URLEncoder.encode(aptName, StandardCharsets.UTF_8), URLEncoder.encode(serviceType, StandardCharsets.UTF_8)
        );

        log.info("ZigBangAptIdApiResource Resource URL ::: " + url);

        try {
            return new UrlResource(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failed to Create ZigBangAptIdApiResource UrlResource");
        }
    }
}
