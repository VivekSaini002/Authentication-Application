package com.auth.config;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private JwtAuthenticationFilter jwtAuthenticationFilter;
	private AuthenticationSuccessHandler successHandler;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			AuthenticationSuccessHandler successHandler) {
		
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.successHandler = successHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {

		return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
				        .requestMatchers(
				                "/api/auth/**",
				                "/oauth2/**",
				                "/login/oauth2/**",
				                "/error"
				        ).permitAll().anyRequest().authenticated())
				.oauth2Login(oauth2 ->
					oauth2.successHandler(successHandler)
					.failureHandler(null)
				).logout(AbstractHttpConfigurer::disable)
			
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
					authException.printStackTrace();
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json");
					String errorMsg = authException.getMessage();
					String error = (String) request.getAttribute("error");
					if (error != null) {
						errorMsg = error;
					}

					Map<String, String> errorMap = Map.of("message", errorMsg, "status", String.valueOf(401),
							"statusCode", Integer.toString(401));
					var objectMapper = new ObjectMapper();
					response.getWriter().write(objectMapper.writeValueAsString(errorMap));
				})).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class).build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
