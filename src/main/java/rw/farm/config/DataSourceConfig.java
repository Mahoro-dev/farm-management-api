package rw.farm.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rw.farm.security.AppUserDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public BeanPostProcessor appUserDataSourceWrapper() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if ("dataSource".equals(beanName) && bean instanceof DataSource dataSource
                        && !(bean instanceof AppUserDataSource)) {
                    return new AppUserDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
