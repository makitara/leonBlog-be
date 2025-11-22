package com.example.myblog.config;

import com.example.myblog.data.BlogDataStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BlogDataStore dataStore;

    public WebConfig(BlogDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        var dataRootLocation = dataStore.dataRoot().toUri().toString();
        if (!dataRootLocation.endsWith("/")) {
            dataRootLocation += "/";
        }
        registry.addResourceHandler(BlogDataStore.ASSET_URL_PREFIX + "**")
                .addResourceLocations(dataRootLocation);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowedHeaders("*");
    }
}
