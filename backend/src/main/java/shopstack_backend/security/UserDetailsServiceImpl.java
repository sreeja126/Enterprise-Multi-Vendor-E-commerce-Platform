package shopstack_backend.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import shopstack_backend.entity.Role;
import shopstack_backend.entity.User;
import shopstack_backend.repository.UserRepository;
import shopstack_backend.repository.VendorRepository;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, VendorRepository vendorRepository) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        // Multi-role support: an account's primary role (User.role) is
        // always granted, but an account can ALSO hold a Vendor profile
        // independent of that primary role — e.g. a CUSTOMER who chose to
        // "Become a Vendor" later. If so, grant VENDOR authority too, so
        // the same login can pass both hasRole("VENDOR") on /api/vendor/**
        // and the normal customer-facing endpoints, without a second account.
        Set<String> roleNames = new LinkedHashSet<>();
        roleNames.add(user.getRole().name());

        if (user.getRole() != Role.VENDOR && vendorRepository.existsByUser(user)) {
            roleNames.add(Role.VENDOR.name());
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(roleNames.toArray(new String[0]))
                .build();
    }
}
