package com.boot.user.service;

import com.boot.user.dto.UserDto;
import com.boot.user.entity.UserEntity;

public interface UserService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByUserId(String userId);
    Iterable<UserEntity> getUserByAll();
}
