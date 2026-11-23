package com.mishcon.portal.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mishcon.portal")
public class RequestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestApplication.class, args);
    }
}
