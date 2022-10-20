# cloud-service
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

그럼, FeignClient 를 사용하여 UserServiceApp <-> OrderServiceApp 간의 통신을 진행해보자. 먼저 `@FeignClient` 어노테이션을 추가한다.
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

## 참고
https://wildeveloperetrain.tistory.com/172
