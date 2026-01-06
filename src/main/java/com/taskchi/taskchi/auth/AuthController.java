package com.taskchi.taskchi.auth;

import com.taskchi.taskchi.users.User;
import com.taskchi.taskchi.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    // مهم: این ریپازیتوری کانتکست را داخل سشن ذخیره می‌کند
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/login")
    public void login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }

    public record MeResponse(Long id, String fullName, String email, List<String> roles) {}

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null) return null;

        String email = authentication.getName();
        User u = userRepository.findByEmail(email).orElse(null);
        if (u == null) return null;

        return new MeResponse(
                u.getId(),
                u.getFullName(),
                email,
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }
}
