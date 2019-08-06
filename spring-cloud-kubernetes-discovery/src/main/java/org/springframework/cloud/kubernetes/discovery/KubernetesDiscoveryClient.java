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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.util.stream.Collectors.toMap;

/**
 * Kubeneretes implementation of {@link DiscoveryClient}.
 *
 * @author Ioannis Canellos
 */
public class KubernetesDiscoveryClient implements DiscoveryClient {

	private static final Log log = LogFactory.getLog(KubernetesDiscoveryClient.class);

	private final KubernetesDiscoveryProperties properties;

	private final DefaultIsServicePortSecureResolver isServicePortSecureResolver;

	private final KubernetesClientServicesFunction kubernetesClientServicesFunction;

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final SimpleEvaluationContext evalCtxt = SimpleEvaluationContext
			.forReadOnlyDataBinding().withInstanceMethods().build();

	private KubernetesClient client;

	public KubernetesDiscoveryClient(KubernetesClient client,
			KubernetesDiscoveryProperties kubernetesDiscoveryProperties,
			KubernetesClientServicesFunction kubernetesClientServicesFunction) {

		this(client, kubernetesDiscoveryProperties, kubernetesClientServicesFunction,
				new DefaultIsServicePortSecureResolver(kubernetesDiscoveryProperties));
	}

	KubernetesDiscoveryClient(KubernetesClient client,
			KubernetesDiscoveryProperties kubernetesDiscoveryProperties,
			KubernetesClientServicesFunction kubernetesClientServicesFunction,
			DefaultIsServicePortSecureResolver isServicePortSecureResolver) {

		this.client = client;
		this.properties = kubernetesDiscoveryProperties;
		this.kubernetesClientServicesFunction = kubernetesClientServicesFunction;
		this.isServicePortSecureResolver = isServicePortSecureResolver;
	}

	public KubernetesClient getClient() {
		return this.client;
	}

	public void setClient(KubernetesClient client) {
		this.client = client;
	}

	@Override
	public String description() {
		return "Kubernetes Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		Assert.notNull(serviceId,
				"[Assertion failed] - the object argument must not be null");

		Endpoints endpoints = this.client.endpoints().withName(serviceId).get();
		List<EndpointSubset> subsets = getSubsetsFromEndpoints(endpoints);
		List<ServiceInstance> instances = new ArrayList<>();
		if (!subsets.isEmpty()) {

			// 子集合不为空
			final Service service = this.client.services().withName(serviceId).get();

			final Map<String, String> serviceMetadata = new HashMap<>();
			// 获得k8s元数据
			KubernetesDiscoveryProperties.Metadata metadataProps = this.properties
					.getMetadata();
			if (metadataProps.isAddLabels()) {
				// k8s label 标签在元数据中
				Map<String, String> labelMetadata = getMapWithPrefixedKeys(
						service.getMetadata().getLabels(),
						metadataProps.getLabelsPrefix());
				if (log.isDebugEnabled()) {
					log.debug("Adding label metadata: " + labelMetadata);
				}
				serviceMetadata.putAll(labelMetadata);
			}
			if (metadataProps.isAddAnnotations()) {
				// ServiceInstance 中包含k8s 注解
				// 结果加上前缀
				Map<String, String> annotationMetadata = getMapWithPrefixedKeys(
						service.getMetadata().getAnnotations(),
						metadataProps.getAnnotationsPrefix());
				if (log.isDebugEnabled()) {
					log.debug("Adding annotation metadata: " + annotationMetadata);
				}
				serviceMetadata.putAll(annotationMetadata);
			}

			for (EndpointSubset s : subsets) {
				// 使用每个端点端口信息扩展服务元数据映射（如果请求）
				Map<String, String> endpointMetadata = new HashMap<>(serviceMetadata);
				if (metadataProps.isAddPorts()) {
					Map<String, String> ports = s.getPorts().stream()
						// 过滤端口名不为空的
							.filter(port -> !StringUtils.isEmpty(port.getName()))
							.collect(toMap(EndpointPort::getName,
									port -> Integer.toString(port.getPort())));
					Map<String, String> portMetadata = getMapWithPrefixedKeys(ports,
							metadataProps.getPortsPrefix());
					if (log.isDebugEnabled()) {
						log.debug("Adding port metadata: " + portMetadata);
					}
					endpointMetadata.putAll(portMetadata);
				}

				List<EndpointAddress> addresses = s.getAddresses();
				for (EndpointAddress endpointAddress : addresses) {
					String instanceId = null;
					if (endpointAddress.getTargetRef() != null) {
						// 获得instanceId
						instanceId = endpointAddress.getTargetRef().getUid();
					}

					// 找到主端口
					EndpointPort endpointPort = findEndpointPort(s);
					instances.add(new KubernetesServiceInstance(instanceId, serviceId,
							endpointAddress, endpointPort, endpointMetadata,
							this.isServicePortSecureResolver
									.resolve(new DefaultIsServicePortSecureResolver.Input(
											endpointPort.getPort(),
											service.getMetadata().getName(),
											service.getMetadata().getLabels(),
											service.getMetadata().getAnnotations()))));
				}
			}
		}

		return instances;
	}

	private EndpointPort findEndpointPort(EndpointSubset s) {
		List<EndpointPort> ports = s.getPorts();
		EndpointPort endpointPort;
		if (ports.size() == 1) {
			endpointPort = ports.get(0);
		}
		else {
			Predicate<EndpointPort> portPredicate;
			if (!StringUtils.isEmpty(properties.getPrimaryPortName())) {
				// 如果设置了给定名称的端口，即为主端口
				// 判断端口名称是否是主端口
				portPredicate = port -> properties.getPrimaryPortName()
						.equalsIgnoreCase(port.getName());
			}
			else {
				portPredicate = port -> true;
			}
			endpointPort = ports.stream().filter(portPredicate).findAny()
					.orElseThrow(IllegalStateException::new);
		}
		return endpointPort;
	}

	private List<EndpointSubset> getSubsetsFromEndpoints(Endpoints endpoints) {
		if (endpoints == null) {
			return new ArrayList<>();
		}
		if (endpoints.getSubsets() == null) {
			return new ArrayList<>();
		}

		return endpoints.getSubsets();
	}

	//返回一个包含原始地图所有条目的新地图
	//但前缀是键
	//如果前缀为null或为空，则返回地图本身（当然不变）
	private Map<String, String> getMapWithPrefixedKeys(Map<String, String> map,
			String prefix) {
		if (map == null) {
			return new HashMap<>();
		}

		// 当前缀为空时，只返回具有相同条目的map
		if (!StringUtils.hasText(prefix)) {
			return map;
		}

		final Map<String, String> result = new HashMap<>();
		map.forEach((k, v) -> result.put(prefix + k, v));

		// 循环加上前缀，返回
		return result;
	}

	@Override
	public List<String> getServices() {
		String spelExpression = this.properties.getFilter();
		Predicate<Service> filteredServices;
		if (spelExpression == null || spelExpression.isEmpty()) {
			filteredServices = (Service instance) -> true;
		}
		else {
			Expression filterExpr = this.parser.parseExpression(spelExpression);
			filteredServices = (Service instance) -> {
				Boolean include = filterExpr.getValue(this.evalCtxt, instance,
						Boolean.class);
				if (include == null) {
					return false;
				}
				return include;
			};
		}
		return getServices(filteredServices);
	}

	public List<String> getServices(Predicate<Service> filter) {
		return this.kubernetesClientServicesFunction.apply(this.client).list().getItems()
				.stream().filter(filter).map(s -> s.getMetadata().getName())
				.collect(Collectors.toList());
	}

}
