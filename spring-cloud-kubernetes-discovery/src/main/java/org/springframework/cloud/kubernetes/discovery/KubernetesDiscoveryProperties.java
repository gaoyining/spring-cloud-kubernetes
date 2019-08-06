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

package org.springframework.cloud.kubernetes.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.style.ToStringCreator;

/**
 * Kubernetes discovery properties.
 *
 * @author Ioannis Canellos
 */
@ConfigurationProperties("spring.cloud.kubernetes.discovery")
public class KubernetesDiscoveryProperties {

	/** If Kubernetes Discovery is enabled. */
	private boolean enabled = true;

	/** The service name of the local instance. */
	@Value("${spring.application.name:unknown}")
	private String serviceName = "unknown";

	/**
	 * SpEL expression to filter services AFTER they have been retrieved from the
	 * Kubernetes API server.
	 */
	private String filter;

	/** Set the port numbers that are considered secure and use HTTPS. */
	private Set<Integer> knownSecurePorts = new HashSet<Integer>() {
		{
			add(443);
			add(8443);
		}
	};

	/**
	 *如果设置，则只从Kubernetes API服务器获取与这些标签匹配的服务。
	 */
	private Map<String, String> serviceLabels = new HashMap<>();

	/**
	 * 如果设置，则在为服务定义多个端口时，将具有给定名称的端口用作主端口。
	 */
	private String primaryPortName;

	private Metadata metadata = new Metadata();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getFilter() {
		return this.filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public Set<Integer> getKnownSecurePorts() {
		return this.knownSecurePorts;
	}

	public void setKnownSecurePorts(Set<Integer> knownSecurePorts) {
		this.knownSecurePorts = knownSecurePorts;
	}

	public Map<String, String> getServiceLabels() {
		return this.serviceLabels;
	}

	public void setServiceLabels(Map<String, String> serviceLabels) {
		this.serviceLabels = serviceLabels;
	}

	public String getPrimaryPortName() {
		return primaryPortName;
	}

	public void setPrimaryPortName(String primaryPortName) {
		this.primaryPortName = primaryPortName;
	}

	public Metadata getMetadata() {
		return this.metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("enabled", this.enabled)
				.append("serviceName", this.serviceName).append("filter", this.filter)
				.append("knownSecurePorts", this.knownSecurePorts)
				.append("serviceLabels", this.serviceLabels)
				.append("metadata", this.metadata).toString();
	}

	/**
	 * 元数据属性。
	 */
	public class Metadata {

		/**
		 * 设置后，服务的Kubernetes标签将作为元数据包含在内
		 * 返回的ServiceInstance。
		 */
		private boolean addLabels = true;

		/**
		 * 设置addLabels后，这将用作键名称的前缀
		 * 元数据map。
		 */
		private String labelsPrefix;

		/**
		 *设置后，将包含服务的Kubernetes 注解
		 *返回的ServiceInstance的元数据。
		 */
		private boolean addAnnotations = true;

		/**
		 *当设置addAnnotations时，它将用作键名的前缀
		 *在元数据map中。
		 */
		private String annotationsPrefix;

		/**
		 * 设置后，任何已命名的Kubernetes服务端口都将作为元数据包含在内
		 * 返回的ServiceInstance。
		 */
		private boolean addPorts = true;

		/**
		 * 设置addPorts后，这将用作键名称的前缀
		 * 元数据map。
		 */
		private String portsPrefix = "port.";

		public boolean isAddLabels() {
			return this.addLabels;
		}

		public void setAddLabels(boolean addLabels) {
			this.addLabels = addLabels;
		}

		public String getLabelsPrefix() {
			return this.labelsPrefix;
		}

		public void setLabelsPrefix(String labelsPrefix) {
			this.labelsPrefix = labelsPrefix;
		}

		public boolean isAddAnnotations() {
			return this.addAnnotations;
		}

		public void setAddAnnotations(boolean addAnnotations) {
			this.addAnnotations = addAnnotations;
		}

		public String getAnnotationsPrefix() {
			return this.annotationsPrefix;
		}

		public void setAnnotationsPrefix(String annotationsPrefix) {
			this.annotationsPrefix = annotationsPrefix;
		}

		public boolean isAddPorts() {
			return this.addPorts;
		}

		public void setAddPorts(boolean addPorts) {
			this.addPorts = addPorts;
		}

		public String getPortsPrefix() {
			return this.portsPrefix;
		}

		public void setPortsPrefix(String portsPrefix) {
			this.portsPrefix = portsPrefix;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("addLabels", this.addLabels)
					.append("labelsPrefix", this.labelsPrefix)
					.append("addAnnotations", this.addAnnotations)
					.append("annotationsPrefix", this.annotationsPrefix)
					.append("addPorts", this.addPorts)
					.append("portsPrefix", this.portsPrefix).toString();
		}

	}

}
