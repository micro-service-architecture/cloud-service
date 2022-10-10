package com.boot.user.service;

import com.boot.user.dto.UserDto;
import com.boot.user.entity.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByUserId(String userId);

    UserDto getUserDetailsByEmail(String username);
    Iterable<UserEntity> getUserByAll();
}
