# boot-user-service
## 목차
* **[사용자 서비스](#사용자-서비스)**
    * **[APIs](#APIs)**
        * **[사용자 정보 등록](#사용자-정보-등록)**
        * **[전체 사용자 조회](#전체-사용자-조회)**
        * **[사용자 정보, 주문 내역 조회](#사용자-정보,-주문-내역-조회)**
    * **[security 연동](#security-연동)**
        * **[X-Frame-Options 헤더](#X-Frame-Options-헤더)**
        * **[인증](#인증)**
        * **[인가](#인가)**
    * **[Routes 정보](#Routes-정보)**
    * **[AuthorizationHeaderFilter](#AuthorizationHeaderFilter)**
* **[애플리케이션 배포 Docker Container](애플리케이션-배포-Docker-Container)**
    * **[UserService 배포](#UserService-배포)**   

## 사용자 서비스
![image](https://user-images.githubusercontent.com/31242766/193811969-0d969c2f-e0d7-4cc2-8933-597192997429.png)
### APIs
|기능|URI (API Gateway 사용시)|URL (API Gateway 미사용시)|HTTP Method|
|----|------------------------|-------------------------|-----------|
|작동 상태 확인|/user-service/users/health_check|/users/health_check|GET|
|환영 메시지|/user-service/users/welcome|/welcome|GET|
|사용자 정보 등록|/user-service/users|/users|POST|
|전체 사용자 조회|/user-service/users|/users|GET|
|사용자 정보, 주문 내역 조회|/user-service/users/{user_id}|/users/{user_id}|GET|
|사용자 로그인|/user-service/login|/login|POST|

### 사용자 정보 등록 
#### request 
```json
{
    "email" : "yong80211@gmail.com",
    "name" : "Haeyong Hahn",
    "pwd" : "qwer1234"
}
```
#### response
![image](https://user-images.githubusercontent.com/31242766/193813545-269e2a08-7fa4-43b4-b2ee-bf9f6ffe79ed.png)

### 전체 사용자 조회
#### response
![image](https://user-images.githubusercontent.com/31242766/194300792-488820e5-459e-4dc3-bd24-e679870fed37.png)

### 사용자 정보, 주문 내역 조회
#### request
- 예시 : localhost:8000/user-service/users/be71131b-fe87-47e7-a21d-53cb1c1ccde6
#### response
![image](https://user-images.githubusercontent.com/31242766/194301354-f4786e1d-1657-4b07-a105-a2454690e0da.png)

## security 연동
```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
//        http.authorizeRequests().antMatchers("/users/**").permitAll();
        http.authorizeRequests().antMatchers("/actuator/**").permitAll();       
        http.authorizeRequests().antMatchers("/health_check/**").permitAll();   
        http.authorizeRequests().antMatchers("/**")                             
                .hasIpAddress("My IP 주소")  // 주어진 IP로부터 요청이 왔다면 접근을 허용한다.
                .and()
                .addFilter(getAuthentionFilter());

        /**
         * X-frame-Options 비활성화
         * 해당 옵션으로 /h2-console 접근가능
         */
        http.headers().frameOptions().disable();
    }
    
    private AuthenticationFilter getAuthentionFilter() throws Exception {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(authenticationManager(), userService, env);
        return authenticationFilter;
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);
    }
}
```
### X-Frame-Options 헤더
웹 어플리케이션에 HTML 삽입 취약점이 존재하면 공격자는 다른 서버에 위치한 페이지를 <frame>, <iframe>, <object> 등으로 삽입하여 다양한 공격에 사용할 수 있다. 피해자의 입장에서는 링크를 눌렀을 때 의도했던 것과는 다른 동작을 하게 한다하여 이를 `클릭재킹(Clickjacking)`이라 부른다. 웹 페이지를 공격에 필요한 형태로 조작하기 때문에 "사용자 인터페이스 덧씌우기"(User Interface redress) 공격이라고도 부른다.   
    
이런 공격을 다른 웹 브라우저가 일부 해결해줄 수 있는 방안이 "X-Frame-Options" 헤더이다. 이 헤더의 값은 `DENY` `SAMEORGIN` `ALLOW-FROM origin` 을 가질 수 있다.
- DENY : 이 홈페이지는 다른 홈페이지에서 표시할 수 없다.
- SAMEORIGIN : 이 홈페이지는 동일한 도메인의 페이지 내에서만 표시할 수 있다.
- ALLOW-FROM origin : 이 홈페이지는 origin 도메인의 페이지에서 포함하는 것을 허용한다.
    
예를 들어, <iframe>내에 표시되는 것을 허용하지 않을 경우에는 `DENY`, 같은 홈페이지 내부에서만 허용할 경우에는 `SAMEORGIN`, 일부 다른 사이트의 페이지 내에서 표시되는 것을 허용해야 한다면 `ALLOW-FROM origin`을 사용할 수 있다. 다만 `ALLOW-FROM origin`의 경우에는 웹 브라우저에 따라서는 지원하지 않는 경우도 있다.

### 인증
![image](https://user-images.githubusercontent.com/31242766/194869378-42fccacf-9a89-44ee-8ddd-43f9284dc3c8.png)

#### AuthenticationFilter
Spring Security 를 이용한 로그인 요청 발생 시 UsernamePasswordAuthenticationFilter 상속받아 작업을 처리해주는 Custom Filter 클래스이다.
- `attemptAuthentication` `successfulAuthentication` 를 구현

#### loadUserByUsername
사용자의 정보를 담을 객체를 만들고 DB에서 유저 정보를 직접 가져와서 확인해야 한다. `UserDetailsService` 인터페이스에는 DB에서 유저 정보를 불러오는 중요한 메소드가 존재한다. 바로 `loadUserByUsername()` 메소드이다. 이 메소드에서 유저 정보를 불러오는 작업을 하면 된다. `UserDetailsService` 인터페이스를 구현하면 `loadUserByUsername()` 메소드가 오버라이드 될 것이다. 여기에서 사용자의 정보를 가져오면 된다. 가져온 사용자의 정보를 유/무에 따라 예외와 사용자 정보를 리턴하면 된다.

#### request
```json
{
    "email" : "youg1322@naver.com",
    "password" : "qwer1234"
}
```
#### response
![image](https://user-images.githubusercontent.com/31242766/194865470-02146e4a-d774-484f-b578-f1942f618c00.png)

### 인가
![image](https://user-images.githubusercontent.com/31242766/194882645-39bd5c37-e344-403f-8ebf-51c5d1056ad7.png)

최초에 사용자가 로그인하면 JWT 토큰을 발급한다. 사용자가 `로그인` 시점 및 `회원가입` 시점에는 JWT 토큰이 없기 때문에 게이트웨이에서 JWT 를 검증하지 않는다. 이후 사용자는 서버의 API 를 호출하기 위해서 헤더에 정보를 입력한다. 각각의 마이크로서비스들이 JWT 토큰을 검증하는 것이 아니라 게이트웨이 [cloud-gateway](https://github.com/multi-module-project/cloud-system/tree/master/gateway) 에서 검증을 마치고 검증된 요청만 마이크로서비스로 전달한다. 

#### Routes 정보
```yml
...
- id: user-service
  uri: lb://USER-SERVICE
  predicates:
    - Path=/user-service/login
    - Method=POST
  filters:
    - RemoveRequestHeader=Cookie
    - RewritePath=/user-service/(?<segment>.*), /${segment}
- id: user-service
  uri: lb://USER-SERVICE
  predicates:
    - Path=/user-service/users
    - Method=POST
  filters:
    - RemoveRequestHeader=Cookie
    - RewritePath=/user-service/(?<segment>.*), /${segment}
- id: user-service
  uri: lb://USER-SERVICE
  predicates:
    - Path=/user-service/**
    - Method=GET
  filters:
    - RemoveRequestHeader=Cookie
    - RewritePath=/user-service/(?<segment>.*), /${segment}
    - AuthorizationHeaderFilter
...
```
#### AuthorizationHeaderFilter
API 요청 정보에서 JWT 토큰 및 정보를 검증하는 Custom Filter 클래스이다.

## 애플리케이션 배포 Docker Container
### UserService 배포

## 출처
https://webhack.dynu.net/?idx=20161117.003&print=friendly     
https://to-dy.tistory.com/86      
https://cheese10yun.github.io/spring-cloud-gateway/
