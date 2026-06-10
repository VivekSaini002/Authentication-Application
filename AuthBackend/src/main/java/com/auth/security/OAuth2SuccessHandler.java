package com.auth.security;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.auth.entities.Provider;
import com.auth.entities.RefreshToken;
import com.auth.entities.Users;
import com.auth.repositories.RefreshTokenRepository;
import com.auth.repositories.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final UserRepository userRepository;
	private final JwtSecurity jwtService;
	private final CookiesService cookiesService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		logger.info("Successful authentication");
		logger.info(authentication.toString());

		// (inside onAuthenticationSuccess)
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
		if ("google".equals(registrationId)) {
		    String email = oAuth2User.getAttribute("email");
		    String name = oAuth2User.getAttribute("name");
		    String picture = oAuth2User.getAttribute("picture");

		    // Find or create user
		    Users user = userRepository.findByEmail(email)
		        .orElseGet(() -> {
		            String pwd = "";
		            Users newUser = Users.builder()
		                    .email(email).name(name).image(picture).provider(Provider.GOOGLE)
		                    .enable(true).password(pwd)
		                    .build();
		            return userRepository.save(newUser);
		        });

		    // Save refresh token, generate JWT
		    String jti = UUID.randomUUID().toString();
		    RefreshToken refreshTokenOb = RefreshToken.builder()
		        .jti(jti).user(user).revoked(false).createdAt(Instant.now())
		        .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
		        .build();
		    refreshTokenRepository.save(refreshTokenOb);

		    String accessToken = jwtService.generateAccessToken(user);
		    String refreshToken = jwtService.generateRefreshToken(user, jti);
		    cookiesService.attachRefreshCookie(response, refreshToken, (int)jwtService.getRefreshTtlSeconds());

		    // Return JSON with access token
		    response.setContentType("application/json");
		    String json = String.format("{\"accessToken\":\"%s\"}", accessToken);
		    response.getWriter().write(json);
		} else {
		    throw new RuntimeException("Unsupported provider: " + registrationId);
		}
	}
}
