package org.apache.mesos.logstash.config;

import org.apache.mesos.logstash.common.network.NetworkUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

// TODO: Add unit test to assert default settings, URIs and commands

@Component
@ConfigurationProperties
public class FrameworkConfig {

    private static final String LOGSTASH_EXECUTOR_JAR   = "logstash-mesos-executor.jar";

    private static final String LOGSTASH_ZIP = "logstash.zip";

    @NotNull
    @Pattern(regexp = "^zk://.+$")
    private String zkUrl;

    private int zkTimeout = 20000;

    private String frameworkName = "logstash";

    private double failoverTimeout = 31449600;

    private long reconcilationTimeoutMillis;

    private String role = "*";
    private String user = "root";

    private String mesosPrincipal = null;
    private String mesosSecret = null;

    private String javaHome = "";

    @Inject
    private NetworkUtils networkUtils;

    public String getJavaHome() {
        if (!javaHome.isEmpty()) {
            return javaHome.replaceAll("java$", "").replaceAll("/$", "") + "/";
        } else {
            return "";
        }
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getZkUrl() {
        return zkUrl;
    }

    public void setZkUrl(String zkUrl) {
        this.zkUrl = zkUrl;
    }

    public int getZkTimeout() {
        return zkTimeout;
    }

    public void setZkTimeout(int zkTimeout) {
        this.zkTimeout = zkTimeout;
    }

    public String getFrameworkName() {
        return frameworkName;
    }

    public void setFrameworkName(String frameworkName) {
        this.frameworkName = frameworkName;
    }

    public double getFailoverTimeout() {
        return failoverTimeout;
    }

    public void setFailoverTimeout(double failoverTimeout) {
        this.failoverTimeout = failoverTimeout;
    }

    public long getReconcilationTimeoutMillis() {
        return reconcilationTimeoutMillis;
    }

    public void setReconcilationTimeoutMillis(long reconcilationTimeoutMillis) {
        this.reconcilationTimeoutMillis = reconcilationTimeoutMillis;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setMesosPrincipal(String mesosPrincipal) {
        this.mesosPrincipal = mesosPrincipal;
    }

    public String getMesosPrincipal() {
        return mesosPrincipal;
    }

    public void setMesosSecret(String mesosSecret) {
        this.mesosSecret = mesosSecret;
    }

    public String getMesosSecret() {
        return mesosSecret;
    }

    public String getFrameworkFileServerAddress() {
        return networkUtils.addressToString(networkUtils.hostSocket(8080), true);
    }

    public String getLogstashZipUri() {
        return getFrameworkFileServerAddress() + "/" + FrameworkConfig.LOGSTASH_ZIP;
    }

    public String getLogstashExecutorUri() {
        return getFrameworkFileServerAddress() + "/" + FrameworkConfig.LOGSTASH_EXECUTOR_JAR;
    }

    public String getExecutorCommand() {
        return getJavaHome() + "java $JAVA_OPTS -jar ./" + FrameworkConfig.LOGSTASH_EXECUTOR_JAR;
    }

}
