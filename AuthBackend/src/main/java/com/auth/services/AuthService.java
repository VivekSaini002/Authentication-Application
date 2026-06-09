package com.auth.services;

import com.auth.dtos.UserDto;

public interface AuthService {
	
	UserDto registerUser(UserDto userDto);
}
