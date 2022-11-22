# cloud-service
## 목차
* **[마이크로서비스 간의 통신](#마이크로서비스-간의-통신)**
    * **[RestTemplate 이란?](#RestTemplate-이란?)**
        * **[RestTemplate 메소드](#RestTemplate-메소드)**
        * **[UserServiceApp <-> OrderServiceApp](#UserServiceApp-<->-OrderServiceApp)**
    * **[FeignClient 란?](#FeignClient-란?)**
        * **[Feign Client 에서 로그 사용](#Feign-Client-에서-로그-사용)**
        * **[FeignException](#FeignException)**
        * **[ErrorDecoder](#ErrorDecoder)**
    * **[데이터 동기화 문제](#데이터-동기화-문제)**
* **[장애 처리와 마이크로서비스 분산 추적](#장애-처리와-마이크로서비스-분산-추적)**
    * **[CircuitBreaker](#CircuitBreaker)**
    * **[Zipkin](#Zipkin)**
* **[마이크로서비스 모니터링](#마이크로서비스-모니터링)**
    * **[Turbin Server](#Turbin-Server)**
    * **[Micrometer와 Monitoring System](#Micrometer와-Monitoring-System)**
    * **[Prometheus와 Grafana](#Prometheus와-Grafana)**
* **[애플리케이션 배포 Docker Container](#애플리케이션-배포-Docker-Container)**
    * **[Mysql 컨테이너 실행](#Mysql-컨테이너-실행)**
    * **[UserService 배포](#UserService-배포)**
* **[애플리케이션 배포 구성](#애플리케이션-배포-구성)**

## 마이크로서비스 간의 통신
- RestTemplate 사용
- FeignClient 사용

### RestTemplate 이란?
- Srping 3.0 부터 지원하는 Spring HTTP 통신 템플릿이다.
- HTTP 요청 후 JSON, XML, String 과 같은 응답을 받을 수 있는 템플릿이다.
- Blocking I/O 기반의 동기방식을 사용하는 템플릿이다.
- Restful 형식에 맞추어진 템플릿이다.
- Header, Content-Type 등을 설정하여 외부 API 를 호출할 수 있다.
- Server to Server 통신에 사용한다.

#### RestTemplate 메소드
|메서드|HTTP|설명|
|----|------------------------|-------------------------|
|getForObject|GET|HTTP GET 요청 후 결과는 객체로 반환|
|getForEntity|GET|HTTP GET 요청 후 결과는 ResponseEntity로 반환|
|postForLocation|POST|HTTP POST 요청 후 결과는 헤더에 저장된 URL을 반환|
|postForObject|POST|HTTP POST 요청 후 결과는 객체로 반환|
|postForEntity|POST|HTTP POST 요청 후 결과는 ResponseEntity로 반환|
|delete|DELETE|HTTP DELETE 요청|
|headForHeaders|HEADER|HTTP HEAD 요청 후 헤더정보를 반환|
|put|PUT|HTTP PUT 요청|
|patchForObject|PATCH|HTTP PATCH 요청 후 결과는 객체로 반환|
|optionsForAllow|OPTIONS|지원하는 HTTP 메소드를 조회|
|exchange|Any|원하는 HTTP 메소드 요청 후 결과는 ResponseEntity로 반환|
|execute|Any|Request/Response의 콜백을 수정|

#### UserServiceApp <-> OrderServiceApp
UserServiceApp 에서 `@GetMapping("/users/{userId}")` api 를 호출하면, OrderSerivceApp 에서 `/{userId}/orders` api 로 저장되어있는 주문 내역을 가져오도록 통신할 것이다.
먼저, UserServiceApp 에서 `RestTemplate` 을 빈으로 등록하자. 그리고나서 UserService 에서 해당 내용을 구현하자.
```java
public class UserServiceApplication {

    ...
    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
```
#### UserController
```java
@GetMapping("/users/{userId}")
public ResponseEntity<ResponseUser> getUsers(@PathVariable("userId") String userId) {
    UserDto userDto = userService.getUserByUserId(userId);

    ResponseUser returnValue = new ModelMapper().map(userDto, ResponseUser.class);

    return ResponseEntity.status(HttpStatus.OK).body(returnValue);
}
```
### UserService
UserService 에서 OrderSerivce API 를 호출할 때 url 정보가 입력된다. 하지만 url 정보가 달라질 수 있으므로 설정 파일로 뺴서 관리하도록 한다.
```java
@Override
public UserDto getUserByUserId(String userId) {
    UserEntity userEntity = userRepository.findByUserId(userId);

    if(userEntity == null)
        throw new UsernameNotFoundException("User not found");

    UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

//  List<ResponseOrder> orders = new ArrayList<>();

    /**
     * url : http://127.0.0.1:8000/order-service/%s/orders
     * Method : GET
     * parameters : null
     * response : List<ResponseOrder>
     */
    String orderUrl = String.format(env.getProperty("order_service.url"), userId);
    ResponseEntity<List<ResponseOrder>> orderListResponse = restTemplate.exchange(orderUrl, HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ResponseOrder>>() {
    });

    List<ResponseOrder> orderList = orderListResponse.getBody();
    userDto.setOrders(orderList);

    return userDto;
}
```
```yml
# user-service.yml
...
order_service:
  url: http://127.0.0.1:8000/order-service/%s/orders
```

여기서 또 하나의 문제는 현재 `로컬 주소`로 지정되어 있지만 주소 정보도 변할 수가 있다. 그래서 해당 정보는 로드 밸런서를 이용하여 처리한다.
```java
public class UserServiceApplication {

    ...
    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
```
```yml
# user-service.yml
...
order_service:
  url: http://ORDER-SERVICE/order-service/%s/orders
```

#### 테스트 결과
![image](https://user-images.githubusercontent.com/31242766/196743043-cf2bcee5-e6ca-4348-8a8e-e94090644f05.png)

### FeignClient 란?
- REST Call 을 추상화한 Spring Cloud Netflix 라이브러리이다. 
- 호출하려는 HTTP Endpoint 에 대한 Interface 를 생성하여 사용한다.
- @FeignClient 선언하여 사용한다.
- 개발자 입장에서 훨씬 더 직관적으로 하나의 어플리케이션 안에 포함되어 있는 메소드를 호출하는 것처럼 사용할 수 있다.

그럼, FeignClient 를 사용하여 UserServiceApp <-> OrderServiceApp 간의 통신을 진행해보자. 먼저 `@EnableFeignClients` 어노테이션을 추가한다.
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
```
UserService 에 client 패키지를 추가하여 해당 패키지 안에 통신하고자 하는 마이크로서비스명으로 인터페이스를 선언하자.
```java
package com.boot.user.client;

import com.boot.user.vo.ResponseOrder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "order-service") // 해당 name 은 마이크로서비스 명칭으로 지정한다.
public interface OrderServiceClient {

    @GetMapping("/order-service/{userId}/orders") // OrderService API Url 이다. 
    List<ResponseOrder> getOrders(@PathVariable String userId); // 반환값은 OrderService 해당 API 의 반환값이다.
}
```
그리고 UserService 에서 `RestTemplate` 을 이용한 것처럼 해당 `getUserByUserId` 메소드에서 API 를 가져온다. 소스상에서 보는 것처럼 `feignClient` 를 사용하여 메소드처럼 사용할 수 있으며 소스 길이도 확실히 짧아졌다.
```java
@Override
public UserDto getUserByUserId(String userId) {
    UserEntity userEntity = userRepository.findByUserId(userId);

    if(userEntity == null)
        throw new UsernameNotFoundException("User not found");

    UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

//  List<ResponseOrder> orders = new ArrayList<>();

    /* Using as Rest Template */
    /**
     * url : http://127.0.0.1:8000/order-service/%s/orders
     * Method : GET
     * parameters : null
     * response : List<ResponseOrder>
     */
    // String orderUrl = String.format(env.getProperty("order_service.url"), userId);
    // ResponseEntity<List<ResponseOrder>> orderListResponse = restTemplate.exchange(orderUrl, HttpMethod.GET,
    //        null,
    //        new ParameterizedTypeReference<List<ResponseOrder>>() {
    // });
    // List<ResponseOrder> orderList = orderListResponse.getBody();
    
    /* Using a feign client */
    List<ResponseOrder> orderList = orderServiceClient.getOrders(userId);
    
    userDto.setOrders(orderList);

    return userDto;
}
```
#### 테스트 결과
![image](https://user-images.githubusercontent.com/31242766/196944360-a467dc9a-9273-49f5-8e58-7382d40faf3e.png)

#### Feign Client 에서 로그 사용
`로그`와 `예외처리` 에서 사용되는 서버는 여전히 `UserServiceApp` 과 `OrderServiceApp` 이다.    
먼저, `application.yml` 파일에서 로깅 레벨을 정의한다. 그리고 `feign 클래스` 에 존재하는 `Logger` 를 빈으로 등록하자.
```yml
...
logging:
  level:
    com.boot.user.client: DEBUG <- feignClient 가 존재하는 패키지 (com.boot.user.client)
...
```
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class UserServiceApplication {
    ...
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
```
- `Logger.Leverl.FULL` : Request와 Response의 Header, Body 그리고 메타데이터를 로깅한다.
- `Logger.Leverl.NONE` : 로깅하지 않는다. (DEFAULT)
- `Logger.Leverl.BASIC` : Request Method와 URL 그리고 Reponse 상태 코드와 실행 시간을 로깅한다.
- `Logger.Leverl.HEADERS` : Request, Response Header 정보와 함께 BASIC 정보를 로깅한다.

잘못된 주소로 OrderServiceApp API 를 호출했을 때 로그 결과를 확인할 수 있다.
```java
// OrderService API 에 존재하는 정상적인 경로는 /order-service/{userId}/orders 이다.
@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/order-service/{userId}/orders_ng")
    List<ResponseOrder> getOrders(@PathVariable String userId);
}
```

![image](https://user-images.githubusercontent.com/31242766/196950334-9ca8ef0b-9a6c-438f-8fd4-878f97fedd80.png)

#### FeignException
UserServiceApp 에서 OrderServiceApp 으로 API 를 호출할 때 잘못된 주소로 호출한다고 하자. 그런데, OrderServiceApp 으로 호출할 때 잘못된 주소로 호출해서 User 정보까지 반환이 안되는 경우는 없어야 한다. 결론은 사용자 정보는 출력이 되면서 Order 정보만 표시하지 않도록 해야한다. 문제가 생긴 부분은 해결을 해야겠지만 문제가 생기지 않은 부분은 반환해주어야 한다.
```java
...
List<ResponseOrder> orderList = null;
try {
    orderList = orderServiceClient.getOrders(userId);
} catch (FeignException ex) {
    log.error(ex.getMessage());
}
...
```
아래 이미지처럼 OrderServiceApp API 를 호출할 때, 에러를 발생하고 User 정보를 반환한다.

![image](https://user-images.githubusercontent.com/31242766/196958143-a32cc8fc-bce7-4dde-ac99-81459ddf35ba.png)

![image](https://user-images.githubusercontent.com/31242766/196958326-c4b3a6a9-0f2c-49b4-bc87-15ea4a19bd67.png)

#### ErrorDecoder 
`ErrorDecoder` 인터페이스의 `decode` 메소드가 존재하는데 클라이언트 측에서 발생했던 에러 상태 코드 분기를 통해 작업할 수 있도록 지원을 해준다. 위의 작업에서 FeignException Log 를 확인하기 위해서 `try-catch` 문을 작성한 것을 주석처리하고 `ErrorDecoder` 를 이용해보자. 상태 코드(404, 400...) 와 methodKey(FeignClient Url) 를 통해서 경로가 이상하다면 외부 설정에 등록되어 있는 오류 메시지와 함께 오류를 던지도록 한다.
```java
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    Environment env;

    @Autowired
    public FeignErrorDecoder(Environment env) {
        this.env = env;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:
                break;
            case 404:
                if(methodKey.contains("getOrders")) {
                    return new ResponseStatusException(HttpStatus.valueOf(response.status()),
                            env.getProperty("order_service.exception.orders_is_empty"));
                }
                break;
            default:
                return new Exception(response.reason());
        }
        return null;
    }
}
```
```yml
# user-service.yml

...
order_service:
  # url: http://127.0.0.1:8000/order-service/%s/orders
  url: http://ORDER-SERVICE/order-service/%s/orders
  exception:
    orders_is_empty: User's orders is empty.
```
다음과 같이 메시지와 상태코드를 던져준다. 만약 trace 정보와 message 정보가 출력되지 않다면? `org.springframework.boot:spring-boot-devtools` 를 디펜던시에 추가한다.

![image](https://user-images.githubusercontent.com/31242766/197248761-6b76f435-bf47-4097-b791-f6c213e7f9ef.png)

FeignClient 를 여러 개 선언 후 각 Interface 마다 별도의 ErrorDecoder 를 설정할 수 있다. (configuration 속성)
```java
@FeignClient(name = "order-service", configuration = FeignErrorDecoder.class)
public interface OrderServiceClient {

    @GetMapping("/order-service/{userId}/orders_ng")
    List<ResponseOrder> getOrders(@PathVariable String userId);
}
```

## 데이터 동기화 문제
[Multi Orders Service](https://github.com/multi-module-project/cloud-service/tree/master/boot-order-service) 를 사례로 확인해보자.

## 장애 처리와 마이크로서비스 분산 추적
### CircuitBreaker
- https://martinfowler.com/bliki/CircuitBreaker.html
- 장애가 발생하는 서비스에 반복적인 호출이 되지 못하게 차단
- 특정 서비스가 정상적으로 동작하지 않을 경우 다른 기능으로 대체 수행 -> 장애 회피

CircuitBreaker는 두 가지 용도로 기억할 수 있다. 하나는 `Open`이고 또 하나는 `Closed`이다. `CircuitBreaker Closed`되었다면 정상적으로 `다른 마이크로서비스를 사용할 수 있다`라는 의미이다. 예를 들어, UserSerivce에서 OrderSerivce를 사용함에 있어 아무런 문제가 없다면 `CircuitBreaker Closed`상태이다. UserService에서 OrderService로 이용이 불가한 상태가 된다면 `CircuitBreaker Open`상태가 된다. `CircuitBreaker Open`이 되면 UserSerivce가 OrderSerivce로 내용을 전달하지 않고 CircuitBreaker에서 자체적으로 기본값 또는 우회할 수 있는 값을 가지고 리턴시켜주는 작업을 진행한다. 이전에 만들었던 마이크로서비스에 CircuitBreaker를 추가시켜줌으로써 연쇄적으로 연결되어있는 다른 마이크로서비스에 문제가 발생했다하더라도 해당하는 마이크로서비스만큼은 정상적으로 작동할 수 있게끔 만들어줄 수 있다.

![image](https://user-images.githubusercontent.com/31242766/201461954-10489749-9df8-455c-9d90-c0e8d31e060e.png)

#### Spring Cloud Netflix Hystrix
![image](https://user-images.githubusercontent.com/31242766/201462591-d3999580-2309-4c72-be09-4c3da471d34e.png)

2019년도 이후부터 Hystrix가 개발되어지지 않고 유지보수만 하고 있는 상태이다. 그리고 이제는 유지보수 또한 끊긴다고 한다. 그래서 Spring Boot 2.3x 버전이라고 한다면 해당 라이브러리를 사용할 수 있지만 Spring Boot가 2.4x 이상이고 Spring Cloud 2020.x 이상을 사용한다고 한다면 Hystrix 라이브러리가 더 이상 제공되지 않기 때문에 대체할 수 있는 다른 라이브러리로 대체해야한다. 

![image](https://user-images.githubusercontent.com/31242766/201462694-9b4bde9a-49d9-488f-9317-ffc2d89187f9.png)

#### Resilience4j
Java 전용으로 개발된 경량화된 Fault Tolerance(장애감내) 제품이다. Resilience4j는 아래 6가지 핵심모듈로 구성되어 있다.
- Circuit Breaker : Count(요청건수 기준) 또는 Time(집계시간 기준)으로 Circuit Breaker 제공
- Bulkhead : 각 요청을 격리함으로써, 장애가 다른 서비스에 영향을 미치지 않게 함(bulkhead-격벽이라는 뜻)
- RateLimiter : 요청의 양을 조절하여 안정적인 서비스를 제공
- Retry : 요청이 실패하였을 때 재시도하는 기능 제공
- TimeLimiter : 응답시간이 지정된 시간을 초과하면 Timeout을 발생시켜준다
- Cache : 응답 결과를 캐싱하는 기능 제공

이전 내용에서 마이크로서비스 통신 간 장애가 발생했을 시 `ErrorDecoder` 를 사용하여 오류를 던질 수 있도록 구성했다. 하지만 이번엔 `Resilience4j`를 사용하여 `OrderService`에 오류가 발생했더라도 `UserService`만큼은 정상적으로 작동할 수 있게끔 구성해보자.
- UserServiceImpl 수정 전, 후 비교       
`userId`를 받아 사용자의 주문 내역을 반환해주는 함수이다. `ErrorDecoder`를 사용하여 FeignClient의 orderService에 오류가 발생 시 오류를 반환할 수 있도록 구성했었다. 하지만 수정 후 오류를 잡아주되 UserService의 데이터는 반환될 수 있도록 수정했다.
```java
@Service
@Slf4j
public class UserServiceImpl implements UserService {
   ...
   @Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null)
            throw new UsernameNotFoundException("User not found");

        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

//        List<ResponseOrder> orders = new ArrayList<>();

        /* Using as Rest Template */
        /**
         * url : http://127.0.0.1:8000/order-service/%s/orders
         * Method : GET
         * parameters : null
         * response : List<ResponseOrder>
         */
//        String orderUrl = String.format(env.getProperty("order_service.url"), userId);
//        ResponseEntity<List<ResponseOrder>> orderListResponse = restTemplate.exchange(orderUrl, HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<ResponseOrder>>() {
//        });
//        List<ResponseOrder> orderList = orderListResponse.getBody();

        /* Using a feign client */
//        List<ResponseOrder> orderList = null;
//        try {
//            orderList = orderServiceClient.getOrders(userId);
//        } catch (FeignException ex) {
//            log.error(ex.getMessage());
//        }

        /* ErrorDecoder */
        List<ResponseOrder> orderList = orderServiceClient.getOrders(userId);

        userDto.setOrders(orderList);

        return userDto;
    }

}
```
`orderServiceClient.getOrders(userId)`에 오류가 발생했다면 `new ArrayList<>()`를 반환한다.
```java
@Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null)
            throw new UsernameNotFoundException("User not found");

        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

//        List<ResponseOrder> orders = new ArrayList<>();

        /* Using as Rest Template */
        /**
         * url : http://127.0.0.1:8000/order-service/%s/orders
         * Method : GET
         * parameters : null
         * response : List<ResponseOrder>
         */
//        String orderUrl = String.format(env.getProperty("order_service.url"), userId);
//        ResponseEntity<List<ResponseOrder>> orderListResponse = restTemplate.exchange(orderUrl, HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<ResponseOrder>>() {
//        });
//        List<ResponseOrder> orderList = orderListResponse.getBody();

        /* Using a feign client */
//        List<ResponseOrder> orderList = null;
//        try {
//            orderList = orderServiceClient.getOrders(userId);
//        } catch (FeignException ex) {
//            log.error(ex.getMessage());
//        }

        /* ErrorDecoder */
//        List<ResponseOrder> orderList = orderServiceClient.getOrders(userId);

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitbreaker");
        List<ResponseOrder> orderList = circuitBreaker.run(() -> orderServiceClient.getOrders(userId),
                throwable -> new ArrayList<>());

        userDto.setOrders(orderList);

        return userDto;
    }
```
![image](https://user-images.githubusercontent.com/31242766/201580889-24e812fe-0b46-400a-b36b-0705b8b5de2c.png)

![image](https://user-images.githubusercontent.com/31242766/201580921-819f7a62-306c-489d-954f-2d3962acb2a8.png)

- Resilience4j Config 설정
```java
CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
       .failureRateThreshold(4)
       .waitDurationInOpenState(Duration.ofMillis(1000))
       .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
       .slidingWindowSize(2)
       .build();
```
- failureRateThreshold : CircuitBreaker를 열지 결정하는 failure rate threshold 퍼센트 (기본값 : 50)
- waitDurationInOpenState : CircuitBreaker를 open한 상태를 유지하는 지속 기간을 의미. 이 기간 이후에 half-open 상태 (기본값 : 60초)
- slidingWindowType : CircuitBreaker가 닫힐 때 통화 결과를 기록하는 데 사용되는 슬라이딩 창의 유형을 구성. 카운트 기반 또는 시간 기반
- slidingWindowSize : CircuitBreaker가 닫힐 때 호출 결과를 기록하는 데 사용되는 슬라이딩 창의 크기를 구성 (기본값 : 100)

```java
TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
       .timeoutDuration(Duration.ofSeconds(4))
       .build();
```
- timeoutDuration : TimeLimiter는 future supplier의 time limit을 정하는 API (기본값 : 1초)

### Zipkin
하나의 서비스 시작이 되고 끝날 때까지 다양한 형태의 마이크로서비스가 연결될 수 있기 때문에 사용자 요청이 어디를 거쳐서 어떻게 진행되어 왔는지, 누가 문제가 되었는지 시각적으로. 또는 로그 파일로 파악할 수 있는 서비스가 필요하게 되었다. 그래서 Zipkin이라는 서비스를 이용해서 해당하는 부분을 처리해보려고 한다.

- https://zipkin.io/
- Twitter에서 사용하는 분산 환경의 Timing 데이터 수집, 추적 시스템(오픈소스)
- Google Drapper에서 발전하였으며, 분산환경에서의 시스템 병목 현상 파악
- Collector, Query Service, Database WebUI로 구성
- Span
   - 하나의 요청에 사용되는 작업의 단위
   - 64 bit unique ID
- Trace
   - 트리 구조로 이뤄진 Span 셋
   - 하나의 요청에 대한 같은 Trace ID 발급
   
#### Zipkin 아키텍처
1. Reporter가 Transport를 통해서 Collector에 트레이스 정보를 전달한다.
2. 전달된 트레이스 정보는 Database에 저장된다.
3. Zipkin UI에서 API를 통해 해당 정보를 시각화해서 제공한다.

각 컴포넌트를 조금 더 자세히 알아보자.

![image](https://user-images.githubusercontent.com/31242766/202846934-d7704a5e-bf83-4544-aafc-dc1eb8df880a.png)
- Reporter
   - 각 서버는 계측(instrumented) 라이브러리를 사용해야 Reporter로서 동작할 수 있다. Zipkin에서는 다양한 언어에 대한 라이브러리를 제공하고 있다.([참고](https://zipkin.io/pages/tracers_instrumentation.html))
- Database
   - Zipkin에서 몇 가지 Storage를 지원하고 있다.([참고](https://github.com/openzipkin/zipkin#storage-component))
- Zipkin
   - 웹에서 제공하는 명령어를 실행하는 것만으로 간단하게 설치할 수 있고 여기 약간의 설정을 추가해서 ES와 같은 사용할 수 있다.
```shell
curl -sSL https://zipkin.io/quickstart.sh | bash -s <- 다운로드 명령어
java -jar zipkin.jar --STORAGE_TYPE=elasticsearch --ES_HOSTS=http://127.0.0.1:9200
```
![image](https://user-images.githubusercontent.com/31242766/202847725-e0e9ee6b-fc74-4b8d-9dff-3eea9c826e8f.png)

![image](https://user-images.githubusercontent.com/31242766/202847185-ac09f268-0f32-4133-bbfd-29339e4123c7.png)

#### Spring Cloud Sleuth
- 스프링 부트 애플리케이션을 Zipkin과 연동한다.
- 요청 값에 따른 Trace ID, Span ID를 부여한다.
- Trace와 Span ID을 로그에 추가 가능하다. 아래와 같은 곳에서 Trace와 Span ID를 발생시켜 추적 정보로 사용할 수 있다.
   - servlet filter
   - rest template
   - scheduled actions
   - message channels
   - feign client

#### Spring Cloud Sleuth + Zipkin
먼저, `마이크로서비스 C`로 갈 수도 있지만 `마이크로서비스 D`로 갈 수 있다고 가정해보자. 처음에 사용자가 하나의 요청을 `마이크로서비스 A`로 전달한다. 이 때 Trace ID(`AA`)가 발급이된다. 그리고 처음 발생한 Span ID(`AA`)는 같은 걸로 발급이 된다. 이 후, 요청된 작업이 끝날 때까지는 같은 Trace ID(`AA`)가 사용이 된다. `마이크로서비스 B` 에서 `마이크로서비스 C`로 요청할 때에는 Trace ID(`AA`)는 그대로이며, Span ID(`BB`)는 새로 발급이 된다. `마이크로서비스 C`에서 `마이크로서비스 D`를 호출할 때에도 마찬가지이다. 

![image](https://user-images.githubusercontent.com/31242766/202846343-9f16401b-096b-44a4-9bd9-75db9e051639.png)

#### Spring Cloud Sleuth + Zipkin 구현
- application.yml 설정
```yml
...
spring:
  application:
    name: user-service
  zipkin:
    base-url: http://127.0.0.1:9411 # 데이터를 전송할 zipkin 서버 url
    enabled: true
  sleuth:
    sampler:
      probability: 1.0 # 어플리케이션으로 오늘 요청 중 초당 몇 퍼센트나 트렌잭션 정보를 외부로 전달할지 설정한다. 0.0 ~ 1.0 값을 사용할 수 있으며 디폴트는 0.1 (10%)이다.
...
```
- orderService log 확인
```java
@PostMapping("/{userId}/orders")
public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
                                               @RequestBody RequestOrder orderDetails) {
  log.info("Before add orders data");
  ModelMapper mapper = new ModelMapper();
  mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

  OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
  orderDto.setUserId(userId);

  /* jpa */
  OrderDto createdOrder = orderService.createOrder(orderDto);
  ResponseOrder responseOrder = mapper.map(createdOrder, ResponseOrder.class);

  /* kafka */
  //orderDto.setOrderId(UUID.randomUUID().toString());
  //orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

  /* send this order to the kafka */
  //kafkaProducer.send("example-catalog-topic", orderDto);
  //orderProducer.send("orders", orderDto);

  //ResponseOrder responseOrder = mapper.map(orderDto, ResponseOrder.class);

  log.info("After added orders data");
  return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
}
```
![image](https://user-images.githubusercontent.com/31242766/202897775-d1de6d4a-8fd2-4da4-91dc-28d87624eeb0.png)

![tempsnip](https://user-images.githubusercontent.com/31242766/202897584-9f078016-4caa-49a7-9911-5fe12c3cdd5c.png)

![image](https://user-images.githubusercontent.com/31242766/202897678-623f7b7e-fd57-4f82-ac0f-3ed6eae29625.png)

- userService log 확인
```java
@Override
public UserDto getUserByUserId(String userId) {
  UserEntity userEntity = userRepository.findByUserId(userId);

  if(userEntity == null)
      throw new UsernameNotFoundException("User not found");

  UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

//List<ResponseOrder> orders = new ArrayList<>();

  /* Using as Rest Template */
  /**
   * url : http://127.0.0.1:8000/order-service/%s/orders
   * Method : GET
   * parameters : null
   * response : List<ResponseOrder>
   */
//String orderUrl = String.format(env.getProperty("order_service.url"), userId);
//ResponseEntity<List<ResponseOrder>> orderListResponse = restTemplate.exchange(orderUrl, HttpMethod.GET,
//    null,
//    new ParameterizedTypeReference<List<ResponseOrder>>() {
//});
//List<ResponseOrder> orderList = orderListResponse.getBody();

  /* Using a feign client */
//List<ResponseOrder> orderList = null;
//try {
//  orderList = orderServiceClient.getOrders(userId);
//} catch (FeignException ex) {
//   log.error(ex.getMessage());
//}

  /* ErrorDecoder */
//List<ResponseOrder> orderList = orderServiceClient.getOrders(userId);

  log.info("Before call orders microservice");
  CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitbreaker");
  List<ResponseOrder> orderList = circuitBreaker.run(() -> orderServiceClient.getOrders(userId),
          throwable -> new ArrayList<>());
  log.info("After called orders microservice");

  userDto.setOrders(orderList);

  return userDto;
}
```
![image](https://user-images.githubusercontent.com/31242766/202897962-799eceda-d73d-472c-a95d-10f80833aff8.png)

![image](https://user-images.githubusercontent.com/31242766/202898026-dddfde77-b24b-48db-8683-c52ea936b006.png)

![image](https://user-images.githubusercontent.com/31242766/202898102-d66715d9-abf0-4041-bc95-edd7a1612eb1.png)

- 장애 발생 확인     
장애를 발생시켜 zipkin을 통해 확인해보자.
```java
@GetMapping("/{userId}/orders")
public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) throws Exception {
  log.info("Before retrieve orders data");
  Iterable<OrderEntity> orderList = orderService.getOrdersByUserId(userId);

  List<ResponseOrder> result = new ArrayList<>();
  orderList.forEach(v -> {
      result.add(new ModelMapper().map(v, ResponseOrder.class));
  });

  try {
      Thread.sleep(1000);
      throw new Exception("장애 발생");
  } catch(InterruptedException e) {
      log.warn(e.getMessage());
  }
  log.info("After retrieved orders data");
  return ResponseEntity.status(HttpStatus.OK).body(result);
}
```
![image](https://user-images.githubusercontent.com/31242766/202898548-424ee9ac-f18b-407d-ae76-f943d0426db0.png)

#### Zipkin 결론
마이크로서비스는 하나의 완벽한 애플리케이션을 가지고 구동하는 것이 아니라, 여러 개의 유기적인 서비스를 연결하여 사용한다. 그래서 데이터를 확인할 때 어떤 메소드가 어떤 요청으로 연결이 되었는지 파악하는 것이 중요하다. 수십만 개 마이크로서비스로 연결되어 있을 때 중간에 문제가 생기는 것이 있는지, 어느 쪽에서 병목 현상이 일어나는지 Zipkin을 통해서 유추할 수 있다. 여기서 더 나아가 각각의 마이크로서비스가 현재 가지고 있는 메모리 상태라던지. 호출되어 있는 정확한 횟수라던지. 이러한 것을 파악하기 위해 `Prometheus` 등 모니터링 기능을 수행하는 것을 알아보자. 

## 마이크로서비스 모니터링
### Turbin Server
Spring-Cloud 버전 2020.x 이전에는 각종 마이크로서비스에서 발생하는 상황, 성능을 모니터링 하기 위해서 기본적으로 `Hystrix` 또는 `Turbin Server`를 구성해서 사용했다. `Turbin Server`라는 것은 마이크로서비스에서 발생하는 각종 로그, 결과값들을 Hystrix 클라이언트의 스트림을 통해서 전송되어진 내용들을 모으고 로그 파일처럼 저장하고 있다가 Hystrix 대시보드라든지, 다른 모니터링 도구에 전달하는 역할을 수행했다. 
```yml
...
#Turbin Server
turbine:
   appConfig:
      msa-service-product-order,
      msa-service-product-member,
      msa-service-product-status
   clusterNameExpression: new String("default")
...
```
![image](https://user-images.githubusercontent.com/31242766/202899149-d8158f39-fa9a-4763-83d5-d1483658b13b.png)

### Hystrix Dashboard
- Hystrix 클라이언트에서 생성하는 스트림을 시각화
   - Web Dashboard
   ![image](https://user-images.githubusercontent.com/31242766/202899218-d2cd32dd-1df3-4d19-90f6-57d738739cfc.png)

Circuit Breaker 정보 등 등을 알 수 있다. 하지만 단점이 존재한다. 웹 애플리케이션으로 기동되다보니 리소스를 많이 차지할 수 밖에 없었고 도식화되어 있는 정보가 Serial로 데이터를 보관하지 못하고 현재 발생했던 단편적인 내용만 보여주기 때문에 어제 발생했던 것이라든지, 지난 시간에 발생했던 데이터를 확인하기 위해 추가적으로 데이터베이스와 연동하는 작업이 필요하다. 

![image](https://user-images.githubusercontent.com/31242766/202899404-c1064081-ca9e-432e-bbab-0f7f6f2e7cd9.png)

### Micrometer와 Monitoring System
![image](https://user-images.githubusercontent.com/31242766/202899546-d79b25c9-14fc-4a78-a1f7-fd0f4d3b33c2.png)

#### Micrometer
- Micrometer
   - https://micrometer.io/
   - JVM 기반의 애플리케이션의 Metrics 제공
   - Spring Framework 5, Spring Boot 2부터 Spring의 Metrics 처리
   - Prometheus 등의 다양한 모니터링 시스템 지원
- Timer
   - 짧은 지연 시간, 이벤트의 사용 빈도를 측정
   - 시계열로 이벤트의 시간, 호출 빈도 등을 제공
   - @Timed 제공

#### Micrometer 구현
- `spring cloud actuator` 정보에 `info` `metrics` `prometheus`를 추가해보자.
```yml
management:
  endpoints:
    web:
      exposure:
        include: refresh, health, beans, busrefresh, info, metrics, prometheus
```
- micrometer는 메소드 호출에 대한 타이밍 정보를 수집하는 데 사용할 수 있는 @Timed 어노테이션을 추가하자.
```java
@GetMapping("/welcome")
@Timed(value = "users.welcome", longTask = true)
public String welcome() {
  // =return env.getProperty("greeting.message");
  return greeting.getMessage();
}
```
```java
@GetMapping("/health_check")
@Timed(value = "users.status", longTask = true)
public String status() {
  return String.format("It's Working in User Service"
          + ", port(local.server.port)=" + env.getProperty("local.server.port")
          + ", port(server.port)=" + env.getProperty("server.port")
          + ", token secret=" + env.getProperty("token.secret")
          + ", token expiration time=" + env.getProperty("token.expiration_time"));
}
```
- http://localhost:53203/actuator/metrics `metircs` 정보 확인     
```json
{
  "names": [
    "application.ready.time",
    "application.started.time",
    "disk.free",
    "disk.total",
    "executor.active",
    "executor.completed",
    "executor.pool.core",
    "executor.pool.max",
    "executor.pool.size",
    "executor.queue.remaining",
    "executor.queued",
    "hikaricp.connections",
    "hikaricp.connections.acquire",
    "hikaricp.connections.active",
    "hikaricp.connections.creation",
    "hikaricp.connections.idle",
    "hikaricp.connections.max",
    "hikaricp.connections.min",
    "hikaricp.connections.pending",
    "hikaricp.connections.timeout",
    "hikaricp.connections.usage",
    "http.server.requests",
    "jdbc.connections.max",
    "jdbc.connections.min",
    "jvm.buffer.count",
    "jvm.buffer.memory.used",
    "jvm.buffer.total.capacity",
    "jvm.classes.loaded",
    "jvm.classes.unloaded",
    "jvm.gc.live.data.size",
    "jvm.gc.max.data.size",
    "jvm.gc.memory.allocated",
    "jvm.gc.memory.promoted",
    "jvm.gc.overhead",
    "jvm.gc.pause",
    "jvm.memory.committed",
    "jvm.memory.max",
    "jvm.memory.usage.after.gc",
    "jvm.memory.used",
    "jvm.threads.daemon",
    "jvm.threads.live",
    "jvm.threads.peak",
    "jvm.threads.states",
    "logback.events",
    "process.cpu.usage",
    "process.start.time",
    "process.uptime",
    "rabbitmq.acknowledged",
    "rabbitmq.acknowledged_published",
    "rabbitmq.channels",
    "rabbitmq.connections",
    "rabbitmq.consumed",
    "rabbitmq.failed_to_publish",
    "rabbitmq.not_acknowledged_published",
    "rabbitmq.published",
    "rabbitmq.rejected",
    "rabbitmq.unrouted_published",
    "spring.data.repository.invocations",
    "spring.integration.channels",
    "spring.integration.handlers",
    "spring.integration.sources",
    "system.cpu.count",
    "system.cpu.usage",
    "tomcat.sessions.active.current",
    "tomcat.sessions.active.max",
    "tomcat.sessions.alive.max",
    "tomcat.sessions.created",
    "tomcat.sessions.expired",
    "tomcat.sessions.rejected",
    "users.status",                       <- @Timed 어노테이션에서 추가하면 metrics 정보에 추가되어 지표를 수집할 때 사용이 된다.
    "users.welcome",                      <- @Timed 어노테이션에서 추가하면 metrics 정보에 추가되어 지표를 수집할 때 사용이 된다.
    "zipkin.reporter.messages",
    "zipkin.reporter.messages.total",
    "zipkin.reporter.queue.bytes",
    "zipkin.reporter.queue.spans",
    "zipkin.reporter.spans",
    "zipkin.reporter.spans.dropped",
    "zipkin.reporter.spans.total"
  ]
}
```
- http://localhost:53203/actuator/prometheus `prometheus` 정보 확인    
@Timed 어노테이션에 들어가 있는 함수의 지표이다. 함수가 몇 번 호출되었는지, 사용되어 있는 시간이 어느정도 되는지 등 등이다.

![image](https://user-images.githubusercontent.com/31242766/202902864-2192f1b9-72a3-4ff0-b3a9-2976f1462ee5.png)
![image](https://user-images.githubusercontent.com/31242766/202902916-4d585819-0e52-4258-8e3f-2ac7f3c27178.png)

### Prometheus와 Grafana
#### Prometheus
- https://prometheus.io/download/
- Metrics를 수집하고 모니터링 및 알람에 사용되는 오픈소스 애플리케이션
- 2016년부터 CNCF(Cloud Native Computer Fundation) 단체에서 관리되는 2번째 공식 프로젝트
   - Level DB -> Time Series Database(TSDB) 즉, 각종 지표가 시간 순으로 정보를 담는다라고 생각하면 될 것 같다.
- Pull 방식의 구조와 다양한 Metric Exporter 제공, Java 뿐만 아니라 다양한 언어 지원을 한다.
- 시계열 DB에 Metric 저장 -> 조회 가능(Query)

#### Grafana
- 데이터 시각화, 모니터링 및 분석을 위한 오픈소스 애플리케이션
- 시계열 데이터를 시각화하기 위한 대시보드 제공

![image](https://user-images.githubusercontent.com/31242766/202903327-98b7e879-3a21-4d1b-bf38-f4f695b8b609.png)

## 애플리케이션 배포 Docker Container
### Mysql 컨테이너 실행
```docker
docker run -d -p 13306:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=true --name mariadb mariadb:10.5.17

-d : 백그라운드 모드로 실행
-p : 포트 포워딩 (앞에 있는 PC는 Host PC에서 접근하고자 하는 포트, 뒤에 있는 포트는 Container에서 응답하기 위한 포트이다.
-e : 환경 변수 셋팅을 위한 옵션. "MYSQL_ALLOW_EMPTY_PASSWORD=true" 을 주어 password를 입력하지 않도록 설정을 부여함
--name : "mysql:5.7" 이라는 이미지에 "mysql" 컨테이너라는 이름을 부여했다.
```
```docker
docker exec -it mariadb /bin/bash

exec : 컨테이너에 어떤 커맨드를 전달하고자 할 때 사용하는 명령어이다.
-it : 실행되어 있는 컨테이너에 커맨드를 전달하고자 할 때 사용하는 옵션
```
![image](https://user-images.githubusercontent.com/31242766/203329687-26d5eb15-ec04-4287-aae2-75bebe57db6a.png)

#### Host PC에서 접근
![image](https://user-images.githubusercontent.com/31242766/203329297-da5fe310-42dd-4176-affa-e9eead2be482.png)

### UserService 배포
[UserService](https://github.com/multi-module-project/cloud-service/tree/master/boot-user-service)에서 알아보자.

## 애플리케이션 배포 구성
다양한 서비스들이 하나의 네트워크를 가질 수 있도록 구성해서 서로 통신할 때 문제없이 동작하려고 한다. 그러기 위해서 도커 네트워크를 만들어 보자.

![image](https://user-images.githubusercontent.com/31242766/203351235-c152c038-a6bb-4b8f-9138-2073b620a247.png)

### Docker Network
- Bridge network     
Host PC와 별도의 가상의 네트워크를 만들고 가상의 네트워크에서 만들어서 사용하는 컨테이너들을 배치하고 사용하는 방식이다. 
```docker
docker network create --driver bridge [브릿지 이름]
```
- Host network
   - 네트워크를 호스트로 설정하면 호스트의 네트워크 환경을 게스트 네트워크에 그대로 사용한다.
   - 포트 포워딩 없이 내부 애플리케이션을 사용할 수 있다.
- None network
   - 네트워크를 사용하지 않는다.
   - 외부와 단절

![image](https://user-images.githubusercontent.com/31242766/203353076-a8bfd938-68bd-4160-ae61-5c082b45cc3c.png)

#### Bridge network 생성
```docker
docker network create --gateway 172.18.0.1 --subnet 172.18.0.0/16 ecommerce-network
```
![image](https://user-images.githubusercontent.com/31242766/203353609-a347f70e-dbb3-4df9-a17b-57ad20732b48.png)

#### network 상세 정보 확인
```docker
docker network inspect ecommerce-network
```
![image](https://user-images.githubusercontent.com/31242766/203353903-4285b590-63b2-488e-bc1d-5a56665b45b3.png)

#### 컨테이너를 사용하기 위한 네트워크를 생성해서 사용하면 좋은 점?    
일반적으로 컨테이너는 하나의 Guest OS이다. 각각의 Guest OS마다 고유한 IP Address가 할당된다. 컨테이너 간에는 이러한 IP Address를 통해서 통신하게 되는데
만약 같은 네트워크에 포함된 컨테이너 간에는 IP Address 외에도 컨테이너 ID, 컨테이너 이름을 통해서 통신할 수 있게 된다.

![image](https://user-images.githubusercontent.com/31242766/203356057-2274533a-d5a8-4906-a466-aff06bb8504a.png)

## 참고
https://wildeveloperetrain.tistory.com/172       
https://stackoverflow.com/questions/54827407/remove-trace-field-from-responsestatusexception      
https://happycloud-lee.tistory.com/219      
https://engineering.linecorp.com/ko/blog/line-ads-msa-opentracing-zipkin/      
https://jinheecong.tistory.com/18
