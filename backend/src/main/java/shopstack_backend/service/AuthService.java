package shopstack_backend.service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import shopstack_backend.dto.AuthResponse;
import shopstack_backend.dto.LoginRequest;
import shopstack_backend.dto.RegisterRequest;
import shopstack_backend.entity.PasswordResetToken;
import shopstack_backend.entity.Role;
import shopstack_backend.entity.User;
import shopstack_backend.entity.Vendor;
import shopstack_backend.repository.PasswordResetTokenRepository;
import shopstack_backend.repository.UserRepository;
import shopstack_backend.repository.VendorRepository;
import shopstack_backend.security.JwtService;
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired(required = false)
    private VendorRepository vendorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

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
        if (request.getRole() == Role.VENDOR && vendorRepository != null) {
            Vendor vendor = new Vendor();
            vendor.setName(user.getFullName());
            vendor.setEmail(user.getEmail());
            vendor.setStatus("APPROVED");
            vendor.setUser(user);
            vendorRepository.save(vendor);
        }
        return "Registration Successful";
    }
    // Login
    public AuthResponse login(LoginRequest request) {

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            return new AuthResponse(null, null, "User not found");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(null, null, "Invalid Password");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, user.getRole().name(), "Login Successful");
    }
    public String forgotPassword(String email) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {

            User user = optionalUser.get();
            passwordResetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = new PasswordResetToken(
                    token,
                    user,
                    LocalDateTime.now().plusMinutes(30)
            );

            passwordResetTokenRepository.save(resetToken);
            System.out.println("PASSWORD RESET LINK for " + user.getEmail() +
                    ": http://localhost:5173/reset-password?token=" + token);

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            } catch (Exception e) {
                System.err.println("Failed to send reset email to " + user.getEmail() + ": " + e.getMessage());
            }
        }

        return "If an account exists for that email, a reset link has been sent.";
    }

    // Reset Password
    public String resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("This reset link has expired. Please request a new one.");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("Password cannot be empty");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Token is single-use — remove it once it's been used.
        passwordResetTokenRepository.delete(resetToken);

        return "Password has been reset successfully";
    }
}