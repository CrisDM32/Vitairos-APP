package com.vitairos.report;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Configuration
class Access implements WebMvcConfigurer, HandlerInterceptor {
    record Profile(String email, String name, String role) {
        boolean supervisor() { return "SUPERVISOR".equals(role); }
    }
    private final RestClient identity;

    Access(RestClient.Builder builder, @Value("${identity.url}") String url) {
        identity = builder.baseUrl(url).build();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/v1/**");
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null) throw new ResponseStatusException(UNAUTHORIZED);
        try {
            Profile user = identity.get().uri("/api/v1/me").header("Authorization", header)
                    .retrieve().body(Profile.class);
            if (user == null) throw new ResponseStatusException(SERVICE_UNAVAILABLE);
            request.setAttribute("user", user);
            return true;
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(UNAUTHORIZED);
        } catch (RestClientException e) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Identidad no disponible");
        }
    }
}
