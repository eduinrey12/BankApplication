package com.devsu.hackerearth.backend.account.config;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 * Filtro de seguridad perimetral HTTP (OWASP Top 10)
 * Inyecta cabeceras defensivas para mitigar Clickjacking, MIME-Sniffing y XSS.
 */
@Component
public class SecurityHeadersFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (response instanceof HttpServletResponse) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.setHeader("X-Content-Type-Options", "nosniff");
			httpResponse.setHeader("X-Frame-Options", "DENY");
			httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
			httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		}
		chain.doFilter(request, response);
	}
}
