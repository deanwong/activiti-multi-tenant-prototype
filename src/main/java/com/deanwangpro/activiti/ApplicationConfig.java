package com.deanwangpro.activiti;

import org.activiti.engine.impl.cfg.multitenant.TenantAwareDataSource;
import org.activiti.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by i311609 on 22/02/2017.
 */
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnBean(TenantInfoHolder.class)
public class ApplicationConfig {

    @Bean
    public DataSource dataSource(@Qualifier("tenantIdentityHolder") TenantInfoHolder tenantInfoHolder) {
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

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "tenants", autowire = Autowire.BY_NAME)
    public List<String> tenants() {
        final List<String> tenants = new ArrayList<>();
        tenants.add("T1");
        tenants.add("T2");
        return tenants;
    }

}
