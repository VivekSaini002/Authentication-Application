package com.auth.serviceImpl;

import java.time.Instant;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.auth.dtos.UserDto;
import com.auth.entities.Provider;
import com.auth.entities.Users;
import com.auth.exceptions.ResourceNotFoundException;
import com.auth.helper.UuidHelper;
import com.auth.repositories.UserRepository;
import com.auth.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
	
	private final UserRepository userRepository;
	private final ModelMapper modelMapper;

	@Override
	public UserDto createUser(UserDto userDto) {
		if(userDto.getEmail() == null || userDto.getEmail().isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}
		
		if(userRepository.existsByEmail(userDto.getEmail())) {
			throw new IllegalArgumentException("Email already exists");
		}
		
		userDto.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
		
		Users user = modelMapper.map(userDto, Users.class);
		Users savedUser = userRepository.save(user);
		
		return modelMapper.map(savedUser, UserDto.class);
	}

	@Override
	public UserDto getUserByEmail(String email) {
		Users user = userRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
		return modelMapper.map(user, UserDto.class);
	
	}

	@Override
	public UserDto updateUser(UserDto userDto, String userId) {
		UUID uuid = UuidHelper.parseUuid(userId);
		Users user = userRepository.findById(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		
		if(userDto.getName() != null) {
			user.setName(userDto.getName());
		}
		
		if(userDto.getPassword() != null) {
			user.setPassword(userDto.getPassword());
		}
		
		if(userDto.getImage() != null) {
			user.setImage(userDto.getImage());
		}
		user.setProvider(userDto.getProvider());
		
		user.setEnable(userDto.isEnable());
		user.setUpdatedAt(Instant.now());
		
		Users updatedUser = userRepository.save(user);
		
		return modelMapper.map(updatedUser, UserDto.class);
	}

	@Override
	public void deleteUser(String userId) {
		UUID uuid = UuidHelper.parseUuid(userId);
		Users user = userRepository.findById(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		userRepository.delete(user);
		
	}

	@Override
	public UserDto getUserById(String userId) {
		UUID uuid = UuidHelper.parseUuid(userId);
		Users user = userRepository.findById(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		return modelMapper.map(user, UserDto.class);
	}

	@Override
	public Iterable<UserDto> getAllUsers() {
		return userRepository.findAll()
				.stream()
				.map(user -> modelMapper.map(user, UserDto.class))
				.toList();
	}

}
