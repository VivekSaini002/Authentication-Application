package com.auth.serviceImpl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth.dtos.UserDto;
import com.auth.services.AuthService;
import com.auth.services.UserService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserService userService;
	
	private final PasswordEncoder passwordEncoder;

	@Override
	public UserDto registerUser(UserDto userDto) {
		userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
		UserDto user = userService.createUser(userDto);
		return user;
	}			

}
