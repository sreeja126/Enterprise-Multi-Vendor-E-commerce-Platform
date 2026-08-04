package shopstack_backend.repository;

import shopstack_backend.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    // Useful helper query methods (optional)
    Optional<Vendor> findByEmail(String email);

    boolean existsByEmail(String email);
}