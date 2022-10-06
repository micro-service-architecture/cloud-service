package com.boot.service.service;

import com.boot.service.dto.UserDto;
import com.boot.service.entity.UserEntity;

public interface UserService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByUserId(String userId);
    Iterable<UserEntity> getUserByAll();
}
