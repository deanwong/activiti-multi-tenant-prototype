package com.deanwangpro.activiti;

import org.activiti.engine.impl.cfg.multitenant.TenantAwareDataSource;
import org.activiti.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.activiti.engine.impl.persistence.StrongUuidGenerator;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Created by i311609 on 22/02/2017.
 */
//@Configuration
//@AutoConfigureAfter(DataSourceAutoConfiguration.class)
//@ConditionalOnBean(TenantInfoHolder.class)
public class MultiTenantMultiSchemaConfiguration {

    @Bean
    public DataSource dataSource(TenantInfoHolder tenantInfoHolder) {
        return new TenantAwareDataSource(tenantInfoHolder);
    }

//    @Bean
//    public ProcessEngineConfigurationConfigurer multiTenantProcessingEngineConfigurer() {
//        return new ProcessEngineConfigurationConfigurer() {
//
//            @Override
//            public void configure(SpringProcessEngineConfiguration config) {
//
//                config.setIdGenerator(new StrongUuidGenerator());
//
//            }
//        };
//    }
}
