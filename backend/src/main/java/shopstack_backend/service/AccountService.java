package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shopstack_backend.dto.AccountInfoResponseDTO;
import shopstack_backend.dto.BecomeVendorRequestDTO;
import shopstack_backend.entity.Role;
import shopstack_backend.entity.User;
import shopstack_backend.entity.Vendor;
import shopstack_backend.repository.UserRepository;
import shopstack_backend.repository.VendorRepository;

import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public AccountInfoResponseDTO getMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return toDTO(user);
    }

    // Lets an existing account (typically a CUSTOMER) gain a Vendor
    // profile WITHOUT creating a second account — the same login then
    // works for both customer browsing and vendor product management.
    @Transactional
    public AccountInfoResponseDTO becomeVendor(String email, BecomeVendorRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.getRole() == Role.VENDOR) {
            throw new IllegalStateException("This account is already registered as a vendor.");
        }
        if (vendorRepository.existsByUser(user)) {
            throw new IllegalStateException("This account already has a vendor profile.");
        }

        Vendor vendor = new Vendor();
        String businessName = (request != null && request.getBusinessName() != null && !request.getBusinessName().isBlank())
                ? request.getBusinessName().trim()
                : user.getFullName();
        vendor.setName(businessName);
        vendor.setEmail(user.getEmail());
        vendor.setPhone(request != null ? request.getPhone() : null);
        vendor.setDescription(request != null ? request.getDescription() : null);
        // Requires admin approval before this vendor can list products —
        // see AdminController's /vendors/{id}/approve|reject. Matches the
        // same policy as a direct vendor registration above.
        vendor.setStatus("PENDING");
        vendor.setUser(user);
        vendorRepository.save(vendor);

        return toDTO(user);
    }

    private AccountInfoResponseDTO toDTO(User user) {
        AccountInfoResponseDTO dto = new AccountInfoResponseDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPrimaryRole(user.getRole().name());

        Optional<Vendor> vendor = vendorRepository.findByUser(user);
        dto.setVendor(user.getRole() == Role.VENDOR || vendor.isPresent());
        vendor.ifPresent(v -> {
            dto.setVendorId(v.getId());
            dto.setVendorStatus(v.getStatus());
        });

        return dto;
    }
}
