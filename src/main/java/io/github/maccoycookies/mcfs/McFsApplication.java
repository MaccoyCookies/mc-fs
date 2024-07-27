package io.github.maccoycookies.mcfs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McFsApplication {

    public static void main(String[] args) {
        SpringApplication.run(McFsApplication.class, args);
    }

    // 1. 基于文件存储的分布式文件系统
    // 2. 块存储 ===> 最常见 效率最高
    // 3. 对象存储

    @Value("${mcfs.path}")
    private String uploadPath;

    @Bean
    ApplicationRunner runner() {
        return args -> {
            FileUtil.init(uploadPath);
            System.out.println("===> McFs started ...");
        };
    }
}
