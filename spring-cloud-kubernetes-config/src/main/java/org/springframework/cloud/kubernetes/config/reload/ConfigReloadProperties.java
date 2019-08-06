/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.config.reload;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置重新加载的常规配置。
 *
 * @author Nicola Ferraro
 */
@ConfigurationProperties(prefix = "spring.cloud.kubernetes.reload")
public class ConfigReloadProperties {

	/**
	 * 在更改时启用Kubernetes配置重新加载。
	 */
	private boolean enabled = false;

	/**
	 * 启用对配置映射的监视以检测更改。
	 */
	private boolean monitoringConfigMaps = true;

	/**
	 * 启用对秘密的监控以检测更改。
	 */
	private boolean monitoringSecrets = false;

	/**
	 * 设置更改时Kubernetes配置重新加载的重新加载策略。
	 */
	private ReloadStrategy strategy = ReloadStrategy.REFRESH;

	/**
	 * 设置Kubernetes配置重新加载的检测模式。
	 */
	private ReloadDetectionMode mode = ReloadDetectionMode.EVENT;

	/**
	 * 设置检测模式为POLLING时使用的轮询周期。
	 */
	private Duration period = Duration.ofMillis(15000L);

	/**
	 * 如果使用重启或关闭策略，Spring Cloud Kubernetes会随机等待
	 *重启前的时间。这样做是为了避免全部
	 *同一应用程序的实例同时重启。这个性质
	 *配置从信号开始的最大等待时间
	 *收到需要重新启动，直到实际重启为止
	 *触发
	 */
	private Duration maxWaitForRestart = Duration.ofSeconds(2);

	public ConfigReloadProperties() {
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isMonitoringConfigMaps() {
		return this.monitoringConfigMaps;
	}

	public void setMonitoringConfigMaps(boolean monitoringConfigMaps) {
		this.monitoringConfigMaps = monitoringConfigMaps;
	}

	public boolean isMonitoringSecrets() {
		return this.monitoringSecrets;
	}

	public void setMonitoringSecrets(boolean monitoringSecrets) {
		this.monitoringSecrets = monitoringSecrets;
	}

	public ReloadStrategy getStrategy() {
		return this.strategy;
	}

	public void setStrategy(ReloadStrategy strategy) {
		this.strategy = strategy;
	}

	public ReloadDetectionMode getMode() {
		return this.mode;
	}

	public void setMode(ReloadDetectionMode mode) {
		this.mode = mode;
	}

	public Duration getPeriod() {
		return this.period;
	}

	public void setPeriod(Duration period) {
		this.period = period;
	}

	public Duration getMaxWaitForRestart() {
		return maxWaitForRestart;
	}

	public void setMaxWaitForRestart(Duration maxWaitForRestart) {
		this.maxWaitForRestart = maxWaitForRestart;
	}

	/**
	 * Reload strategies.
	 */
	public enum ReloadStrategy {

		/**
		 * 触发使用@ConfigurationProperties或@RefreshScope注释的bean的刷新。
		 */
		REFRESH,

		/**
		 * 重新启动Spring ApplicationContext以应用新配置。
		 */
		RESTART_CONTEXT,

		/**
		 * 关闭Spring ApplicationContext以激活重启
		 * 容器。确保绑定了所有非守护程序线程的生命周期
		 * ApplicationContext以及复制控制器或副本集
		 *配置为重新启动pod。
		 */
		SHUTDOWN

	}

	/**
	 * Reload detection modes.
	 */
	public enum ReloadDetectionMode {

		/**
		 * 启用轮询任务，定期检索所有外部属性
		 *当他们改变时重新加载。
		 */
		POLLING,

		/**
		 * 侦听Kubernetes事件并检查配置映射时是否需要重新加载
		 * 或秘密改变。
		 */
		EVENT

	}

}
