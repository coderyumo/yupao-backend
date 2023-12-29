package com.yupi.yupaobackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yupi.yupaobackend.mapper")
public class YuPaoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuPaoBackendApplication.class, args);
    }

}
