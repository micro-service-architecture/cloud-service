# boot-user
## 회원가입
![image](https://user-images.githubusercontent.com/31242766/193811969-0d969c2f-e0d7-4cc2-8933-597192997429.png)
### 회원가입 API
#### request 
```json
{
    "email" : "yong80211@gmail.com",
    "name" : "Haeyong Hahn",
    "pwd" : "qwer1234"
}
```
#### controller
```java
@PostMapping("/users")
public ResponseEntity<ResponseUser> createUser(@RequestBody RequestUser user) {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

    UserDto userDto = mapper.map(user, UserDto.class);
    userService.createUser(userDto);

    ResponseUser responseUser = mapper.map(userDto, ResponseUser.class);

    return ResponseEntity.status(HttpStatus.CREATED).body(responseUser);
}
```
#### service
```java
@Override
public UserDto createUser(UserDto userDto) {
    userDto.setUserId(UUID.randomUUID().toString());

    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    UserEntity userEntity = mapper.map(userDto, UserEntity.class);
    userEntity.setEncryptedPwd(passwordEncoder.encode(userDto.getPwd()));

    userRepository.save(userEntity);

    UserDto returnUserDto = mapper.map(userEntity, UserDto.class);

    return returnUserDto;
}
```
#### db
![image](https://user-images.githubusercontent.com/31242766/193813387-6d740306-f143-4c8c-9853-9dbbe1fc27ed.png)

#### response
![image](https://user-images.githubusercontent.com/31242766/193813545-269e2a08-7fa4-43b4-b2ee-bf9f6ffe79ed.png)

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
