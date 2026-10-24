package com.shop;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class Web implements WebMvcConfigurer {
  @Override public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/"); // ảnh sản phẩm upload
  }
}
