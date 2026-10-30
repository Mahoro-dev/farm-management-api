package rw.farm.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.farm.dto.AccountDtos.AuthResponse;
import rw.farm.dto.AccountDtos.BootstrapRequest;
import rw.farm.dto.AccountDtos.LoginRequest;
import rw.farm.dto.AccountDtos.UserResponse;
import rw.farm.service.AccountService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AccountService accounts;

    @PostMapping("/auth/bootstrap")
    public AuthResponse bootstrap(@Valid @RequestBody BootstrapRequest request) {
        return accounts.bootstrap(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return accounts.login(request);
    }

    @GetMapping("/auth/me")
    public UserResponse me() {
        return accounts.me();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
