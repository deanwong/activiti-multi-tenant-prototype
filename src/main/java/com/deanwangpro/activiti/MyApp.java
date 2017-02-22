package com.deanwangpro.activiti;

import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;

/**
 * Created by i311609 on 22/02/2017.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {SpringProcessEngineConfiguration.class})
public class MyApp {

    public static void main(String[] args) throws SQLException {
        org.h2.tools.Server.createWebServer("-web").start();

        SpringApplication.run(MyApp.class, args);
    }
}
