# boot-user
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

## 전체 사용자 조회
#### response
![image](https://user-images.githubusercontent.com/31242766/194300792-488820e5-459e-4dc3-bd24-e679870fed37.png)

## 사용자 정보, 주문 내역 조회
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
        http.authorizeRequests().antMatchers("/users/**").permitAll();

        /**
         * X-frame-Options 비활성화
         * 해당 옵션으로 /h2-console 접근가능
         */
        http.headers().frameOptions().disable(); 
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
    
## 출처
https://webhack.dynu.net/?idx=20161117.003&print=friendly
