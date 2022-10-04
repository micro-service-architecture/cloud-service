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
    userEntity.setEncryptedPwd("encrypted_password");

    userRepository.save(userEntity);

    UserDto returnUserDto = mapper.map(userEntity, UserDto.class);

    return returnUserDto;
}
```
#### db
![image](https://user-images.githubusercontent.com/31242766/193813387-6d740306-f143-4c8c-9853-9dbbe1fc27ed.png)

#### response
![image](https://user-images.githubusercontent.com/31242766/193813545-269e2a08-7fa4-43b4-b2ee-bf9f6ffe79ed.png)

