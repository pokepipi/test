package com.campus.trading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/** Vue Router history 模式回退: 找不到的页面路径交给 index.html */
@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) return requested;
                        if (path.startsWith("api/") || path.startsWith("uploads/") || path.startsWith("ws/")) return null;
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}