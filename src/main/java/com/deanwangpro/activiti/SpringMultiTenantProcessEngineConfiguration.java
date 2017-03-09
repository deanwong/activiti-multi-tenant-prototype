package com.deanwangpro.activiti;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.asyncexecutor.multitenant.ExecutorPerTenantAsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.multitenant.TenantAwareAsyncExecutor;
import org.activiti.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;

/**
 * Created by i311609 on 06/03/2017.
 */
public class SpringMultiTenantProcessEngineConfiguration extends SpringProcessEngineConfiguration implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringMultiTenantProcessEngineConfiguration.class);

    protected TenantInfoHolder tenantInfoHolder;
    protected boolean booted;

    public SpringMultiTenantProcessEngineConfiguration(TenantInfoHolder tenantInfoHolder) {
        this.tenantInfoHolder = tenantInfoHolder;
    }

    public void registerTenant(String tenantId) {
        if (booted) {
            if (isAsyncExecutorEnabled()) {
                createTenantAsyncJobExecutor(tenantId);
            }
        }
    }

    @Override
    protected void initAsyncExecutor() {

        if (asyncExecutor == null) {
            asyncExecutor = new ExecutorPerTenantAsyncExecutor(tenantInfoHolder);
        }

        super.initAsyncExecutor();

        if (asyncExecutor instanceof TenantAwareAsyncExecutor) {
            for (String tenantId : tenantInfoHolder.getAllTenants()) {
                ((TenantAwareAsyncExecutor) asyncExecutor).addTenantAsyncExecutor(tenantId, false); // false -> will be started later with all the other executors
            }
        }
    }

    @Override
    public ProcessEngine buildProcessEngine() {

        ProcessEngine processEngine = super.buildProcessEngine();

        // Start async executor
        if (asyncExecutor != null) {
            asyncExecutor.start();
        }

        booted = true;
        return processEngine;
    }

    protected void createTenantAsyncJobExecutor(String tenantId) {
        ((TenantAwareAsyncExecutor) asyncExecutor).addTenantAsyncExecutor(tenantId, isAsyncExecutorActivate() && booted);
    }


}
