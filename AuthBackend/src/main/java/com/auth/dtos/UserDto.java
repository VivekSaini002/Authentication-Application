package com.auth.dtos;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.auth.entities.Provider;
import com.auth.entities.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
		
	private UUID id;
	private String name;
	private String email;
	private String password;
	private String image;
	private boolean enable;
	private Instant createdAt;
	private Instant updatedAt;
	private Provider provider;
	
	private Set<RoleDto> role = new HashSet<>();

}
