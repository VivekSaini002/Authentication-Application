package com.auth.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.entities.Users;

public interface UserRepository extends JpaRepository<Users, UUID>{
	
	Optional<Users> findByEmail(String email);
	
	boolean existsByEmail(String email);
	

}
