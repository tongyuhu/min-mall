package com.minimall.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.*;

@Configuration
@MapperScan(
    basePackages = "com.minimall",
    annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class MybatisConfiguration {
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    var value = new MybatisPlusInterceptor();
    value.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return value;
  }
}
