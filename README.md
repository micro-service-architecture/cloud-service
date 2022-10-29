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

## 참고
https://wildeveloperetrain.tistory.com/172       
https://stackoverflow.com/questions/54827407/remove-trace-field-from-responsestatusexception
