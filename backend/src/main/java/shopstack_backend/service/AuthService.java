package shopstack_backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import shopstack_backend.dto.AuthResponse;
import shopstack_backend.dto.LoginRequest;
import shopstack_backend.dto.RegisterRequest;
import shopstack_backend.entity.User;
import shopstack_backend.repository.UserRepository;
import shopstack_backend.security.JwtService;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // Register
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email already exists!";
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);

        return "Registration Successful";
    }

    // Login
    public AuthResponse login(LoginRequest request) {

    Optional<User> optionalUser =
            userRepository.findByEmail(request.getEmail());

    if (optionalUser.isEmpty()) {
        return new AuthResponse(
                null,
                null,
                "User not found");
    }

    User user = optionalUser.get();

    if (!passwordEncoder.matches(
            request.getPassword(),
            user.getPassword())) {

        return new AuthResponse(
                null,
                null,
                "Invalid Password");
    }

    String token = jwtService.generateToken(user.getEmail());

    return new AuthResponse(
            token,
            user.getRole().name(),
            "Login Successful");
}
}