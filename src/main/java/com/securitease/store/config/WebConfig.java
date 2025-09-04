package com.securitease.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web configuration for Spring MVC customizations.
 *
 * <p>This configuration class customizes various aspects of Spring MVC behavior, including pagination parameters and
 * limits to prevent performance issues with large page requests.
 *
 * @author Store Application
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();

        // Set maximum page size to prevent performance issues
        pageableResolver.setMaxPageSize(MAX_PAGE_SIZE);

        // Set default page size
        pageableResolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));

        // Customize parameter names if needed
        pageableResolver.setPageParameterName("page");
        pageableResolver.setSizeParameterName("size");

        resolvers.add(pageableResolver);
    }
}
