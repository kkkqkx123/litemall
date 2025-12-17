package org.linlinjava.litemall.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Admin API 模块的Web配置
 * 
 * 重要说明：本模块不再定义任何Jackson相关配置，完全依赖litemall-core模块的全局Jackson配置。
 * 这样可以确保所有模块使用统一的ObjectMapper配置，避免配置冲突。
 * 
 * 所有Java 8时间类型序列化配置都在litemall-core模块的JacksonConfig中定义。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 配置StringHttpMessageConverter使用UTF-8编码
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        
        // 移除默认的StringHttpMessageConverter，添加我们配置的
        converters.removeIf(converter -> converter instanceof StringHttpMessageConverter);
        converters.add(stringConverter);
        
        // 不再配置MappingJackson2HttpMessageConverter，使用Spring Boot默认配置
        // 这样会自动使用litemall-core模块中配置的ObjectMapper
    }
}