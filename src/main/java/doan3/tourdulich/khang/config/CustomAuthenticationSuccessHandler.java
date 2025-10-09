package doan3.tourdulich.khang.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
@Slf4j

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (role.equals("ROLE_ADMIN")) {
            response.sendRedirect("/admin/statistics");
        } else if (role.equals("ROLE_USER")||role.equals("OAUTH2_USER")) {
            response.sendRedirect("/Client/Home");
        } else {
            response.sendRedirect("/login?error=true");
        }
    }
}
