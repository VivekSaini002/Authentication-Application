package com.auth.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth.entities.Users;
import com.auth.helper.UuidHelper;
import com.auth.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtSecurity jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String authHeader = request.getHeader("Authorization");

            logger.info("Authorization header: {}", authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            // Verify token type
            if (!jwtService.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Parse JWT
            Jws<Claims> claimsJws = jwtService.parse(token);
            Claims claims = claimsJws.getPayload();

            String userId = claims.getSubject();

            if (userId == null || userId.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userUuid = UuidHelper.parseUuid(userId);

            Users user = userRepository.findById(userUuid).orElse(null);

            if (user == null) {
                logger.warn("User not found for token subject {}", userUuid);
                filterChain.doFilter(request, response);
                return;
            }

            // User must be enabled
            if (!user.isEnable()) {
                logger.warn("User account is disabled: {}", user.getEmail());
                filterChain.doFilter(request, response);
                return;
            }

            // Prevent duplicate authentication
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                List<GrantedAuthority> authorities =
                        user.getRole() == null
                                ? List.of()
                                : user.getRole()
                                      .stream()
                                      .map(role ->
                                              new SimpleGrantedAuthority(
                                                      role.getName()))
                                      .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities);

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                logger.info(
                        "User authenticated successfully: {}",
                        user.getEmail());

                logger.info(
                        "Authorities: {}",
                        authorities);
            }

        } catch (ExpiredJwtException e) {

            logger.error("JWT Token Expired", e);
            request.setAttribute("error", "Token Expired");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token Expired");
            return;

        } catch (MalformedJwtException e) {

            logger.error("Malformed JWT", e);
            request.setAttribute("error", "Invalid Token");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid Token");
            return;

        } catch (JwtException e) {

            logger.error("Invalid JWT", e);
            request.setAttribute("error", "Invalid Token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid Token");
            return;

        } catch (Exception e) {

            logger.error("Authentication Error", e);
            request.setAttribute("error", "Invalid Token");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Authentication Error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {

        String path = request.getServletPath();

        return path.startsWith("/api/auth")
                || path.startsWith("/oauth2")
                || path.startsWith("/error");
    }
}






//package com.auth.security;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//import org.slf4j.LoggerFactory;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import org.slf4j.Logger;
//import com.auth.entities.Role;
//import com.auth.helper.UuidHelper;
//import com.auth.repositories.UserRepository;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.Jws;
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.MalformedJwtException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//	private final JwtSecurity jwtService;
//
//	private final UserRepository userRepository;
//
//	private Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
//
//	@Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//			throws ServletException, IOException {
//
//		String header = request.getHeader("Authorization");
//		logger.info("Authorization header: {}", header);
//
//		if (header != null && header.startsWith("Bearer ")) {
//			String token = header.substring(7);
//
//			try {
//				if (!jwtService.isAccessToken(token)) {
//
//					filterChain.doFilter(request, response);
//
//					return;
//				}
//				Jws<Claims> parse = jwtService.parse(token);
//				Claims payload = parse.getPayload();
//				String userId = payload.getSubject();
//				UUID userUuid = UuidHelper.parseUuid(userId);
//				userRepository.findById(userUuid).ifPresent(user -> {
//
//					if (!user.isEnable()) {
//
//						List<GrantedAuthority> authorities = user.getRole() == null ? List.of()
//								: user.getRole().stream().map(role -> new SimpleGrantedAuthority(role.getName()))
//										.collect(Collectors.toList());
//						UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//								user.getEmail(), null, authorities);
//
//						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//						if (SecurityContextHolder.getContext().getAuthentication() == null) {
//							SecurityContextHolder.getContext().setAuthentication(authentication);
//						}
//					}
//				});
//
//			} catch (ExpiredJwtException e) {
//				e.printStackTrace();
//				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//				response.getWriter().write("Token expired");
//				return;
//			} catch (MalformedJwtException e) {
//				e.printStackTrace();
//				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//				response.getWriter().write("Malformed token");
//				return;
//			} catch (JwtException e) {
//				e.printStackTrace();
//				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//				response.getWriter().write("Invalid token");
//				return;
//			} catch (Exception e) {
//				e.printStackTrace();
//				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//				response.getWriter().write("An error occurred while processing the token");
//				return;
//			}
//		}
//
//		filterChain.doFilter(request, response);
//	}
//	
//	@Override
//	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
//		return request.getRequestURI()
//		.startsWith("/api/auth");
//	}
//
//}
