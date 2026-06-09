package com.auth.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.dtos.LoginRequest;
import com.auth.dtos.RefreshTokenRequest;
import com.auth.dtos.TokenResponse;
import com.auth.dtos.UserDto;
import com.auth.entities.RefreshToken;
import com.auth.entities.Users;
import com.auth.repositories.RefreshTokenRepository;
import com.auth.repositories.UserRepository;
import com.auth.security.CookiesService;
import com.auth.security.JwtSecurity;
import com.auth.services.AuthService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final AuthenticationManager authenticationManager;

	private final UserRepository userRepository;

	private final JwtSecurity jwtService;

	private final ModelMapper modelMapper;

	private final RefreshTokenRepository refreshTokenRepository;

	private final CookiesService cookiesService;

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

		Authentication authenticate = authenticate(loginRequest);
		Users user = userRepository.findByEmail(loginRequest.email())
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		if (!user.isEnable()) {
			throw new DisabledException("User is disabled");
		}

		String jti = UUID.randomUUID().toString();
		var refreshTokenOb = RefreshToken.builder().jti(jti).user(user).createdAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds())).revoked(false).build();

		refreshTokenRepository.save(refreshTokenOb);

		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

		cookiesService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());
		cookiesService.addNoStoreHeaders(response);

		TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(),
				modelMapper.map(user, UserDto.class));
		return ResponseEntity.ok(tokenResponse);

	}

	private Authentication authenticate(LoginRequest loginRequest) {
		try {
			return authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

		} catch (Exception e) {
			throw new BadCredentialsException("Invalid email or password");
		}

	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refreshToken(@RequestBody(required = false) RefreshTokenRequest body,
			HttpServletResponse response, HttpServletRequest request) {

		String refreshToken = readRefreshTokenFromRequest(body, request)
				.orElseThrow(() -> new BadCredentialsException("Refresh token is missing"));

		if (!jwtService.isRefreshToken(refreshToken)) {
			throw new BadCredentialsException("Invalid refresh token type");
		}

		String jti = jwtService.getJti(refreshToken);
		UUID userId = jwtService.getUserId(refreshToken);
		RefreshToken storeRefreshToken = refreshTokenRepository.findByJti(jti)
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

		if (storeRefreshToken.isRevoked()) {
			throw new BadCredentialsException("Refresh token is expired or revoked");
		}

		if (storeRefreshToken.getExpiresAt().isBefore(Instant.now())) {
			throw new BadCredentialsException("Refresh token is expired");
		}

		if (!storeRefreshToken.getUser().getId().equals(userId)) {
			throw new BadCredentialsException("Refresh token does not belong to this user");
		}

		storeRefreshToken.setRevoked(true);
		String newJti = UUID.randomUUID().toString();
		storeRefreshToken.setReplacedByToken(newJti);
		refreshTokenRepository.save(storeRefreshToken);

		Users user = storeRefreshToken.getUser();
		var newRefreshTokenOb = RefreshToken.builder().jti(newJti).user(user).createdAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds())).revoked(false).build();

		refreshTokenRepository.save(newRefreshTokenOb);
		String newAccessToken = jwtService.generateAccessToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());

		cookiesService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTtlSeconds());
		cookiesService.addNoStoreHeaders(response);
		return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTtlSeconds(),
				modelMapper.map(user, UserDto.class)));

	}

	private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
		if (request.getCookies() != null) {
			Optional<String> fromCookie = Arrays.stream(request.getCookies())
					.filter(c -> cookiesService.getRefreshTokenCookieName().equals(c.getName())).map(Cookie::getValue)
					.filter(v -> !v.isBlank()).findFirst();

			if (fromCookie.isPresent()) {
				return fromCookie;
			}
		}

		if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
			return Optional.of(body.refreshToken());
		}

		return Optional.empty();
	}

	@PostMapping("/logout")
	public ResponseEntity<Object> logout(HttpServletRequest request, HttpServletResponse response) {
		readRefreshTokenFromRequest(null, request).ifPresent(token -> {
			try {
				if(jwtService.isRefreshToken(token)) {
					String jti = jwtService.getJti(token);
					refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
						rt.setRevoked(true);
						refreshTokenRepository.save(rt);
					});
				}
				
			} catch(JwtException ignored) {	
			}
		});
		
		cookiesService.clearRefreshCookie(response);
		cookiesService.addNoStoreHeaders(response);
		SecurityContextHolder.clearContext();
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@PostMapping("/register")
	public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
		UserDto registeredUser = authService.registerUser(userDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);

	}

}
