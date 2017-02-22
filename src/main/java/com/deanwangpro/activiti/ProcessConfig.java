package com.deanwangpro.activiti;

import org.activiti.engine.*;
import org.activiti.engine.impl.asyncexecutor.multitenant.ExecutorPerTenantAsyncExecutor;
import org.activiti.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.activiti.engine.impl.cfg.multitenant.MultiSchemaMultiTenantProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.multitenant.TenantAwareDataSource;
import org.activiti.spring.SpringExpressionManager;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

/**
 * Created by i311609 on 22/02/2017.
 */
@Configuration
//@Order(LOWEST_PRECEDENCE)
public class ProcessConfig {

    // https://github.com/egovernments/egov-playground
    private static final String BPMN_FILE_CLASSPATH_LOCATION = "classpath:processes/%s/*.bpmn";
    private static final String BPMN20_FILE_CLASSPATH_LOCATION = "classpath:processes/%s/*.bpmn20.xml";

//    @Autowired
//    ProcessAuthConfigurator processAuthConfigurator;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @DependsOn("tenants")
    MultiSchemaMultiTenantProcessEngineConfiguration processEngineConfiguration(TenantIdentityHolder tenantIdentityHolder) {
        MultiSchemaMultiTenantProcessEngineConfiguration processEngineConfig = new MultiSchemaMultiTenantProcessEngineConfiguration(tenantIdentityHolder);

//        processEngineConfig.setDataSource(dataSource);
        processEngineConfig.setDatabaseType(MultiSchemaMultiTenantProcessEngineConfiguration.DATABASE_TYPE_H2);
        processEngineConfig.setDatabaseSchemaUpdate(MultiSchemaMultiTenantProcessEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE);

        processEngineConfig.setAsyncExecutorEnabled(true);
        processEngineConfig.setAsyncExecutorActivate(true);

        processEngineConfig.setAsyncExecutor(new ExecutorPerTenantAsyncExecutor(tenantIdentityHolder));

//        processEngineConfig.setExpressionManager(new SpringExpressionManager(appContext, null));

        processEngineConfig.setExpressionManager(new SpringExpressionManager(applicationContext, processEngineConfig.getBeans()));
        processEngineConfig.setBeans(new SpringBeanFactoryProxyMap(applicationContext));

        // processEngineConfig.setDeploymentMode("resource-parent-folder");
//        processEngineConfig.setConfigurators(Arrays.asList(processAuthConfigurator));
        tenantIdentityHolder.getAllTenants().stream().filter(Objects::nonNull).forEach(tenant ->
                processEngineConfig.registerTenant(tenant, createDataSource("jdbc:h2:mem:activiti-mt-" + tenant + ";DB_CLOSE_DELAY=1000", "sa", ""))
        );

        return processEngineConfig;
    }

    private DataSource createDataSource(String jdbcUrl, String jdbcUsername, String jdbcPassword) {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(jdbcUrl);
        ds.setUser(jdbcUsername);
        ds.setPassword(jdbcPassword);
        return ds;
    }


    @Bean
    ProcessEngine processEngine(MultiSchemaMultiTenantProcessEngineConfiguration processEngineConfiguration,
                                TenantIdentityHolder tenantIdentityHolder) throws IOException {
        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
        ResourceFinderUtil resourceResolver = new ResourceFinderUtil();

        List<Resource> commonBpmnResources =
                resourceResolver.getResources("classpath:processes/common/*.bpmn",
                        "classpath:processes/common/*.bpmn20.xml");

        for (String tenant : tenantIdentityHolder.getAllTenants()) {
            System.out.println(tenant);
            tenantIdentityHolder.setCurrentTenantId(tenant);

            List<Resource> resources = resourceResolver.getResources(format(BPMN_FILE_CLASSPATH_LOCATION, tenant),
                    format(BPMN20_FILE_CLASSPATH_LOCATION, tenant));
            List<String> resourceNames = resources.stream().map(Resource::getFilename).collect(Collectors.toList());
            resources.addAll(commonBpmnResources.stream().
                    filter(rsrc -> !resourceNames.contains(rsrc.getFilename())).
                    collect(Collectors.toList()));
            for (Resource resource : resources) {
                System.out.println(resource.getFilename());
                processEngine.getRepositoryService().createDeployment().
                        enableDuplicateFiltering().name(resource.getFilename()).
                        addInputStream(resource.getFilename(), resource.getInputStream()).deploy();
            }
            tenantIdentityHolder.clearCurrentTenantId();
        }
        return processEngine;
    }

    @Bean
    ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    FormService formService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    @Bean
    IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }

//    @Bean
//    TenantAwareProcessFilter tenantAwareProcessFilter() {
//        return new TenantAwareProcessFilter();
//    }

}
