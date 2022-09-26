package com.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@SpringBootApplication
public class FirstServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FirstServiceApplication.class, args);
    }


}
