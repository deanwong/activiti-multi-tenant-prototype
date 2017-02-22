package com.deanwangpro.activiti;

/**
 * Created by i311609 on 22/02/2017.
 */
public class ProcessEngineThreadLocal {
    private static final ThreadLocal<String> TENANT_THREAD_LOCAL = new ThreadLocal<>();

    public static String getTenant() {
        return TENANT_THREAD_LOCAL.get();
    }

    public static void setTenant(final String tenant) {
        TENANT_THREAD_LOCAL.set(tenant);
    }

    public static void clearTenant() {
        TENANT_THREAD_LOCAL.remove();
        TENANT_THREAD_LOCAL.set(null);
    }
}
