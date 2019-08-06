## Kubernetes Circuit Breaker & Load Balancer Example

此示例演示如何使用[Hystrix断路器]（https://martinfowler.com/bliki/CircuitBreaker.html）
和[功能区负载平衡]（https://microservices.io/patterns/client-side-discovery。 HTML）。
带有功能区支持的断路器将定期检查目标服务是否仍然存在。如果这不是孤立的情况，那么将退出后退流程。
在我们的例子中，如果“名称服务”不再回复，则调用负责生成响应消息的“名称服务”的REST“问候服务”将向客户端回复“回退消息”。
由于在此示例中配置了Ribbon Kubernetes客户端，它将从Kubernetes API服务器获取名称服务可用的端点列表，
并在可用IP地址之间对请求进行负载均衡
### Running the example

此项目示例在所有Kubernetes或OpenShift环境中运行，但出于开发目的，
您可以使用[Minishift  -  OpenShift]（https://github.com/minishift/minishift）
或[Minikube  -  Kubernetes]
（https：// kubernetes。 io / docs / getting-started-guides / minikube /）工具
在VirtualBox，Xhyve或KVM管理的虚拟机中本地安装平台，没有大惊小怪。
### Build/Deploy using Minikube

首先，使用命令“minikube start”在笔记本电脑上创建一个使用Kubernetes配置的新虚拟机。

接下来，您可以编译项目并生成Kubernetes资源（包含pod的定义，部署，构建，服务和要创建的路由的yaml文件）
也喜欢在一个maven系列中部署Kubernetes上的应用程序：

```
mvn clean install fabric8:deploy -Dfabric8.generator.from=fabric8/java-jboss-openjdk8-jdk -Pkubernetes
```

### Call the Greeting service

当maven完成编译代码但也调用平台以便部署生成的yaml文件并告诉平台启动进程
构建/部署docker镜像并创建Spring Boot应用程序将运行'greeting-service'和“name-service”的容器，你将能够
检查是否已使用此命令创建了pod：

```
kc get pods
```

如果Spring Boot pod应用程序的状态为“running”并且状态为“1”，则可以
获取用于从笔记本电脑调用服务的外部地址IP /主机名
```
minikube service --url greeting-service 
```

然后使用curl客户端调用服务
```
curl https://IP_OR_HOSTNAME/greeting
```

得到这样的回应
```
Hello from name-service-1-0dzb4!d
```

### Verify the load balancing

首先，将`name service`的pod数量扩展为2
```
kc scale --replicas=2 deployment name-service
```

等待几分钟后发出curl请求来调用Greeting Service让平台创建新pod。
```
kc get pods --selector=project=name-service
NAME                            READY     STATUS    RESTARTS   AGE
name-service-1652024859-fsnfw   1/1       Running   0          33s
name-service-1652024859-wrzjs   1/1       Running   0          6m
```

如果您发出curl请求以访问问候语服务，则应该看到该消息响应
包含与邮箱名称对应的邮件的不同ID端。
```
Hello from name-service-1-0ss0r!
```

由于Ribbon会询问Kubernetes API，根据`name-service`名称，将作为端点分配给服务的IP地址列表，
你应该看到你将从运行的2个pod中的一个获得响应

```
kc get endpoints/name-service
NAME           ENDPOINTS                         AGE
name-service   172.17.0.5:8080,172.17.0.6:8080   40m
```

这是一个关于你将得到什么的例子
```
curl https://IP_OR_HOSTNAME/greeting
Hello from name-service-1652024859-hf3xv!
curl https://IP_OR_HOSTNAME/greeting
Hello from name-service-1652024859-426kv!
...
```

### Test the fall back

为了测试断路器和回退选项，您将把`name-service`缩放为0 pod
```
kc scale --replicas=0 deployment name-service
```

然后发出新的curl请求以从问候服务获取响应
```
Hello from Fallback!
```
 
### Build/Deploy using Minishift

首先，使用命令`minishift start`在笔记本电脑上创建一个使用OpenShift配置的新虚拟机。

接下来，登录到OpenShift平台，然后在终端内使用`oc`客户端创建一个项目
我们将安装断路器和负载平衡应用

```
oc new-project circuit-loadbalancing
```

使用OpenShift时，必须将“view”角色分配给orde中当前项目中的* default *服务帐户，以允许我们的Java Kubernetes Api访问
API服务器：

```
oc policy add-role-to-user view --serviceaccount=default
```

您现在可以编译项目并生成OpenShift资源（包含pod的定义，部署，构建，服务和要创建的路由的yaml文件）
也喜欢在一个maven系列中在OpenShift平台上部署应用程序：

```
mvn clean install fabric8:deploy -Pkubernetes
```

### Call the Greeting service

当maven完成编译代码但也调用平台以便部署生成的yaml文件并告诉平台启动进程
构建/部署docker镜像并创建Spring Boot应用程序将运行'greeting-service'和“name-service”的容器，你将能够
检查是否已使用此命令创建了pod：

```
oc get pods --selector=project=greeting-service
```

如果Spring Boot pod应用程序的状态为“running”并且状态为“1”，则可以
获取用于从笔记本电脑调用服务的外部地址IP /主机名

```
oc get route/greeting-service 
```

然后使用curl客户端调用服务
```
curl https://IP_OR_HOSTNAME/greeting
```

得到这样的回应
```
Hello from name-service-1-0dzb4!d
```

### Verify the load balancing

首先，将`name service`的pod数量扩展为2
```
oc scale --replicas=2 dc name-service
```

等待几分钟后发出curl请求来调用Greeting Service让平台创建新pod。
```
oc get pods --selector=project=name-service
NAME                   READY     STATUS    RESTARTS   AGE
name-service-1-0ss0r   1/1       Running   0          3m
name-service-1-fblp1   1/1       Running   0          36m
```

如果您发出curl请求以访问问候语服务，则应该看到该消息响应
包含与邮箱名称对应的邮件的不同ID端。

```
Hello from name-service-1-0ss0r!
```

由于Ribbon会询问Kubernetes API，根据`name-service`名称，将作为端点分配给服务的IP地址列表，
您应该会看到，您将从运行的2个pod中的一个获得不同的响应

```
oc get endpoints/name-service
NAME           ENDPOINTS                         AGE
name-service   172.17.0.2:8080,172.17.0.3:8080   40m
```

这是一个关于你将得到什么的例子
```
curl https://IP_OR_HOSTNAME/greeting
Hello from name-service-1-0ss0r!
curl https://IP_OR_HOSTNAME/greeting
Hello from name-service-1-fblp1!
...
```

### Test the fall back

为了测试断路器和回退选项，您将把`name-service`缩放为0 pod
```
oc scale --replicas=0 dc name-service
```

然后发出新的curl请求以从问候服务获取响应
```
Hello from Fallback!
```


