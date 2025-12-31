package com.ads.adshub.config;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class LoggingAspect {

    public void LogCodeExecution(String message) {
        System.out.println("LOG :: " + message);
    }
}
