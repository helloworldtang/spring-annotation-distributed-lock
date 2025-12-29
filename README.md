# Spring Annotation Distributed Lock

基于 Spring 生态与 Redis 的声明式分布式锁组件，支持通过方法注解进行加锁，锁 key 可由 SpringEL 与 @LockKeyParam 共同组成（union all）。

## 特性

- 方法级声明式分布式锁（@Lock）
- 锁 key 组成采用 union all：SpEL keys + @LockKeyParam 参数/字段
- 独立的参数注解模块（lock-key-param），便于在 DTO/请求参数组件中复用
- Spring Boot 自动装配，兼容 Boot 2/3
- Redis 实现，令牌校验释放，支持自旋等待策略

## 依赖与兼容

- Spring Boot：建议 2.7+ / 3.x
- Spring Framework：6.1.12（表达式与上下文在较低版本即可用）
- 组件内部关键依赖使用 `provided`，由业务项目提供实际版本，降低冲突风险

### Java 8 兼容（Boot 2.7）

- 提供 Maven Profile：`boot2-java8`，将栈切换为 Spring Boot 2.7.18 / Spring Framework 5.3.31，并使用 Java 8 编译目标
- 使用方式：
  - 构建：`mvn -P boot2-java8 clean package`
  - 运行示例：`java -jar sample-app/target/sample-app-0.1.0-SNAPSHOT.jar`
  - 集成测试依赖本地 Redis，配置：`spring.data.redis.host` 和 `spring.data.redis.port`

### 构建命令参考

- 默认栈（Boot 3.x / Java 17）

```bash
mvn -pl lock-key-param,distributed-lock-redis-spring -am -DskipTests clean package
```

- Java 8 栈（Boot 2.7.x / Java 8）

```bash
mvn -P boot2-java8 -pl lock-key-param,distributed-lock-redis-spring -am -DskipTests clean package
```

## 快速开始

1. 业务模块引入依赖
   - `lock-key-param`（在参数/DTO 模块）
   - `distributed-lock-redis-spring`（在应用模块）
2. 配置 Redis（Spring Boot 标准属性）
3. 无需编写任何 @Enable\* 注解，只要依赖在 classpath 中，自动装配会创建锁客户端与切面
4. 在方法上使用 @Lock，并在参数或对象字段上标注 @LockKeyParam

示例：

1. 仅 Spring EL

```java
@Lock(prefix = "dl", delimiter = ":", expireTime = 10, waitTime = 3, timeUnit = TimeUnit.SECONDS, keys = {"#p0.userId", "#p1"})
public void place(OrderRequest req, Long orderId) { ... }
```

2. 仅 @LockKeyParam

```java
@Lock(prefix = "dl", delimiter = ":", expireTime = 10, waitTime = 3, timeUnit = TimeUnit.SECONDS)
public void place(@LockKeyParam("userId") OrderRequest req,
                  @LockKeyParam Long orderId) { ... }
```

3. SpEL + @LockKeyParam（union all，先注解后 SpEL）

```java
@Lock(prefix = "dl", delimiter = ":", expireTime = 10, waitTime = 3, timeUnit = TimeUnit.SECONDS, keys = {"#p0.userId", "#p1"})
public void place(@LockKeyParam("userId") OrderRequest req,
                  @LockKeyParam Long orderId) { ... }
```

生成的 key：两者合并（union all），顺序为“先 @LockKeyParam，再 SpEL”，示例：`dl:<@LockKeyParam片段...>:<SpEL片段...>`。

## 常用 SpringEL 示例

- `#user.id`：从参数 user 读取 id
- `#args[0].orderId`：从第一个参数读取 orderId
- `#p0.userId`、`#p1`：基于位置参数的别名变量，`#pN` 等同于 `#args[N]`。p 代表 "parameter"。#p0: 引用第一个参数。#p1: 引用第二个参数。... 以此类推。Spring 3.2+ 支持，依赖参数顺序，代码脆弱
- `#dto.nested.prop`：读取嵌套属性
- `#map['key']`：读取 Map 中的 key
- `#list[0].id`：读取列表首元素的 id

## 锁语义

- 获取：`waitTime > 0` 自旋等待；`waitTime = 0` 快速失败
- 释放：Lua 校验令牌后删除键，避免误删
- 过期：`expireTime` 为租约时间，建议设为业务处理最大时长

### Unlock 设计说明

- 本地令牌缓存：使用 `ThreadLocal<Map<String,String>>` 持有当前线程取得的锁令牌，`unlock` 时先本地取出令牌，不存在则跳过，避免误解锁与跨线程释放
- 原子释放：执行 Lua 脚本 `if get(key)==token then del(key) end`，仅当 Redis 中 key 的值与本地令牌一致时才删除，防止覆盖/误删

### Lua 执行兼容性

- 依赖项：`StringRedisTemplate` 与 `DefaultRedisScript`（spring-data-redis）
- 版本范围：Spring Data Redis 2.7.x（Boot 2.7）与 3.x（Boot 3）均支持 `RedisTemplate#execute(script, keys, args)`；Redis 2.6+ 支持 Lua
- 注意事项：示例使用字符串序列化；若自定义模板需保证 key/value 序列化与脚本参数一致

## 自旋等待策略

- 启用场景：`@Lock(waitTime > 0)` 时进行自旋尝试；`waitTime = 0` 直接快速失败
- 配置项：`@Lock(spinWaitTimeParam = @SpinWaitTimeParam(interval=100, maxAttempts=10, strategy=LINEAR, timeUnit=MILLISECONDS))`
  - interval：基础等待间隔
  - maxAttempts：最多尝试次数（>0 生效）
  - strategy：FIXED（固定）、LINEAR（线性递增）、EXPONENTIAL（指数递增）
  - timeUnit：间隔时间单位
- 策略含义：
  - FIXED：每次等待间隔不变
  - LINEAR：每次在上次基础上 +interval
  - EXPONENTIAL：每次在上次基础上 \*2

## 示例应用

模块 `sample-app` 提供 Spring Boot 示例与集成测试。示例方法采用 `waitTime=0`，并在并发调用时验证第二次快速失败。集成测试使用本地 Redis（可通过 spring.data.redis.host/port 配置），避免对 Docker 的依赖。

## 三种用法与示例

- 仅 SpEL
  - 方法 keys={"#p0.id", "#p1"}，生成 key：`dl:1:9`
  - 测试用例：[KeyResolverTest.onlySpEL](file:///Users/cheng.tang/workspace/codes/ai/spring-annotation-distributed-lock/distributed-lock-redis-spring/src/test/java/com/github/chengtang/dlock/KeyResolverTest.java#L25-L27)
- 仅 @LockKeyParam
  - 方法参数或 DTO 字段标注 @LockKeyParam，生成 key：`dl:1:9`
  - 测试用例（参数级）：[KeyResolverTest.onlyParam](file:///Users/cheng.tang/workspace/codes/ai/spring-annotation-distributed-lock/distributed-lock-redis-spring/src/test/java/com/github/chengtang/dlock/KeyResolverTest.java#L29-L31)
  - 测试用例（字段级）：[KeyResolverTest.onlyField](file:///Users/cheng.tang/workspace/codes/ai/spring-annotation-distributed-lock/distributed-lock-redis-spring/src/test/java/com/github/chengtang/dlock/KeyResolverTest.java#L38-L42)
- SpEL + @LockKeyParam（union all）
  - 顺序：先注解片段，再 SpEL 片段；示例 key：`dl:1:9:1:9`
  - 测试用例：[KeyResolverTest.unionBoth](file:///Users/cheng.tang/workspace/codes/ai/spring-annotation-distributed-lock/distributed-lock-redis-spring/src/test/java/com/github/chengtang/dlock/KeyResolverTest.java#L33-L36)

## 测试与稳定性

- 单元测试：KeyResolver 支持 SpEL 与 @LockKeyParam 的 union all 组合
- 切面集成测试：验证环绕执行与释放流程
- 自动装配测试：在存在 `StringRedisTemplate` 时提供客户端与切面
- 示例应用集成测试：基于 Testcontainers 的真实 Redis 校验
- 依赖冲突防护：核心依赖采用 `provided` scope；通过示例应用验证在业务侧提供依赖时的可用性

## 发布

### Central Portal（推荐，最简）

- 步骤：
  - 在 central.sonatype.com 用 GitHub 登录，创建命名空间（推荐 com.github.helloworldtang）
  - 绑定 GitHub 仓库完成所有权校验，生成发布令牌
  - 补齐 POM 元数据（name/description/url/licenses/developers/scm/issueManagement/organization）
  - 使用 Central 发布插件或官方 CLI，配置令牌后执行发布命令（免 GPG 签名）
  - 发布完成后，Maven Central 即刻可用，mvnrepository 随后收录

### OSSRH（经典流程，GPG+Nexus Staging）

- 步骤：
  - 在 issues.sonatype.org 申请 groupId（推荐 com.github.helloworldtang），完成所有权校验
  - 生成并上传 GPG 公钥（keys.openpgp.org），父 POM 启用 release profile（sources/javadocs/gpg/nexus-staging）
  - 在 ~/.m2/settings.xml 配置 ossrh 凭据
  - 快照：`mvn -Prelease -DskipTests deploy`（进入 snapshots）
  - 正式：`mvn -Prelease -DskipTests deploy` 后在 s01 UI Close + Release（或自动释放）
  - mvnrepository 会自动收录

## 依赖坐标与双栈支持

- 坐标
  - `io.github.helloworldtang:lock-key-param:0.1.0`
  - `io.github.helloworldtang:distributed-lock-redis-spring:0.1.0`
- 单包同时支持 Boot 2/3
  - 同时提供 `spring.factories`（Boot 2）与 `AutoConfiguration.imports`（Boot 3），无需分别发布两个包
  - 自动配置类使用条件注入；核心依赖（Spring Boot/Data Redis）在库内均为 `provided`，不会强制传递到业务侧
  - 示例应用 `sample-app` 已设置跳过发布（避免不必要组件进入仓库）

## Profiles 说明

- boot2-java8
  - 作用：在 Java 8 + Spring Boot 2.7.x/Spring 5.3.x 栈下进行编译与运行兼容性验收
  - 使用：`mvn -P boot2-java8 -pl sample-app -am clean test`
  - 面向维护者验证；业务侧使用时无需启用该 profile
- release
  - 作用：经典 OSSRH 发布流程（sources/javadocs/GPG/Nexus Staging）
  - 使用：`mvn -Prelease -DskipTests deploy`
  - Central Portal 发布（免 GPG）不需要该 profile，可保留作为回退方案
