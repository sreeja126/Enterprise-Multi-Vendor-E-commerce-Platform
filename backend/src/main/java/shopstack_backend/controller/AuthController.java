package shopstack_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import shopstack_backend.dto.AuthResponse;
import shopstack_backend.dto.ForgotPasswordRequest;
import shopstack_backend.dto.LoginRequest;
import shopstack_backend.dto.RegisterRequest;
import shopstack_backend.dto.ResetPasswordRequest;
import shopstack_backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(message);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            String message = authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}