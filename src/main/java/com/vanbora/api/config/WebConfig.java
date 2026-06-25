package com.vanbora.api.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serve estaticamente as imagens enviadas (uploads) a partir do diretório em disco. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String dir;
    private final String publicPath;

    public WebConfig(
            @Value("${vanbora.uploads.dir:uploads}") String dir,
            @Value("${vanbora.uploads.public-path:/uploads}") String publicPath) {
        this.dir = dir;
        this.publicPath = publicPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(dir).toAbsolutePath().normalize();
        registry.addResourceHandler(publicPath + "/**")
                .addResourceLocations(root.toUri().toString());
    }
}
