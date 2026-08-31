package shopstack_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shopstack_backend.entity.ReturnRequest;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    // Customer's own return requests, newest first.
    List<ReturnRequest> findByOrderItem_Order_User_EmailOrderByRequestedAtDesc(String email);

    // Ownership check for a customer creating/viewing their own request.
    Optional<ReturnRequest> findByIdAndOrderItem_Order_User_Email(Long id, String email);

    // Vendor's incoming return requests — only for items on products they
    // own, never another vendor's, newest first.
    List<ReturnRequest> findByOrderItem_Product_Vendor_User_EmailOrderByRequestedAtDesc(String email);

    // Ownership check for a vendor approving/rejecting a request.
    Optional<ReturnRequest> findByIdAndOrderItem_Product_Vendor_User_Email(Long id, String email);

    boolean existsByOrderItemId(Long orderItemId);

    // Admin's global view — every return request, regardless of vendor.
    List<ReturnRequest> findAllByOrderByRequestedAtDesc();
}