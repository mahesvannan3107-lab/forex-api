package com.crewmeister.forex.config;

import com.crewmeister.forex.interceptor.StrictParameterValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StrictParameterValidationInterceptor strictParameterValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(strictParameterValidationInterceptor)
                .addPathPatterns("/api/**");
    }
}
