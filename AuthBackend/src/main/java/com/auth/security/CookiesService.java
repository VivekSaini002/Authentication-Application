package com.auth.security;

import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

@Service
@Getter
public class CookiesService {

	private final String refreshTokenCookieName;
	private final boolean cookieHttpOnly;
	private final boolean cookieSecure;
	private final String cookieDomain;
	private final String cookieSameSite;

	public CookiesService(

			@Value("${security.jwt.refresh-token-cookie-name:refreshToken}") String refreshTokenCookieName,

			@Value("${security.jwt.cookies-http-only:true}") boolean cookieHttpOnly,

			@Value("${security.jwt.cookies-secure:true}") boolean cookieSecure,

			@Value("${security.jwt.cookies-domain:localhost}") String cookieDomain,

			@Value("${security.jwt.cookies-same-site:lax}") String cookieSameSite) {

		this.refreshTokenCookieName = refreshTokenCookieName;
		this.cookieHttpOnly = cookieHttpOnly;
		this.cookieSecure = cookieSecure;
		this.cookieDomain = cookieDomain;
		this.cookieSameSite = cookieSameSite;
	}

	public void attachRefreshCookie(HttpServletResponse response, String value, int maxAge) {

		var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName, value).httpOnly(cookieHttpOnly)
				.secure(cookieSecure).path("/").maxAge(maxAge).sameSite(cookieSameSite);

		if (cookieDomain != null && !cookieDomain.isBlank()) {
			responseCookieBuilder.domain(cookieDomain);
		}

		ResponseCookie responseCookie = responseCookieBuilder.build();
		response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

	}

	public void clearRefreshCookie(HttpServletResponse response) {
		var builder = ResponseCookie.from(refreshTokenCookieName, "").maxAge(0).httpOnly(cookieHttpOnly).path("/")
				.sameSite(cookieSameSite).secure(cookieSecure);
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}
		ResponseCookie responseCookie = builder.build();
		response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

	}

	public void addNoStoreHeaders(HttpServletResponse response) {
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
		response.setHeader(HttpHeaders.PRAGMA, "no-cache");
	}

}
