package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shopstack_backend.dto.RefundResponseDTO;
import shopstack_backend.dto.ReturnRequestResponseDTO;
import shopstack_backend.entity.*;
import shopstack_backend.repository.OrderItemRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.RefundRepository;
import shopstack_backend.repository.ReturnRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnService {

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundService refundService;

    // Customer requests a return — only allowed once an item has actually
    // been DELIVERED (before that, cancellation is the right tool), and
    // only one active request per item.
    @Transactional
    public ReturnRequestResponseDTO requestReturn(String customerEmail, Long orderItemId, String reason) {

        OrderItem item = orderItemRepository.findByIdAndOrder_User_Email(orderItemId, customerEmail)
                .orElseThrow(() -> new SecurityException(
                        "This order item doesn't exist or doesn't belong to your account."));

        if (item.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Returns can only be requested for delivered items. " +
                    "This item is currently " + item.getStatus() + " — cancel it instead if it hasn't shipped yet.");
        }

        if (returnRequestRepository.existsByOrderItemId(orderItemId)) {
            throw new IllegalStateException("A return request already exists for this item.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Please provide a reason for the return.");
        }

        ReturnRequest request = new ReturnRequest();
        request.setOrderItem(item);
        request.setReason(reason);
        request.setStatus(ReturnStatus.REQUESTED);

        return mapToDTO(returnRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestResponseDTO> getMyReturnRequests(String customerEmail) {
        return returnRequestRepository
                .findByOrderItem_Order_User_EmailOrderByRequestedAtDesc(customerEmail)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Only this vendor's own return requests — same isolation pattern as
    // vendor order items.
    @Transactional(readOnly = true)
    public List<ReturnRequestResponseDTO> getVendorReturnRequests(String vendorEmail) {
        return returnRequestRepository
                .findByOrderItem_Product_Vendor_User_EmailOrderByRequestedAtDesc(vendorEmail)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Approving a return: marks the item RETURNED, restores stock, and
    // triggers a real refund. Once the refund itself completes, the item
    // moves to REFUNDED — matching the Pending → ... → Returned → Refunded
    // lifecycle.
    @Transactional
    public ReturnRequestResponseDTO approveReturn(String vendorEmail, Long returnRequestId, String resolutionNote) {

        ReturnRequest request = returnRequestRepository
                .findByIdAndOrderItem_Product_Vendor_User_Email(returnRequestId, vendorEmail)
                .orElseThrow(() -> new SecurityException(
                        "This return request doesn't exist or doesn't belong to your account."));

        if (request.getStatus() != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("This return request has already been " + request.getStatus() + ".");
        }

        OrderItem item = request.getOrderItem();

        // Restore stock — the item is physically coming back.
        Product product = item.getProduct();
        if (product != null) {
            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            product.setStockQuantity(currentStock + item.getQuantity());
            productRepository.save(product);
        }

        item.setStatus(OrderStatus.RETURNED);
        orderItemRepository.save(item);

        request.setStatus(ReturnStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolutionNote(resolutionNote);
        returnRequestRepository.save(request);

        // Trigger the actual refund.
        Refund refund = refundService.processRefund(item);

        if (refund.getStatus() == RefundStatus.PROCESSED) {
            item.setStatus(OrderStatus.REFUNDED);
            orderItemRepository.save(item);
        }
        // If the refund FAILED (gateway error), the item stays RETURNED —
        // physically returned, but payment not yet reversed. That's
        // visible via the Refund record's own FAILED status for follow-up.

        return mapToDTO(request);
    }

    @Transactional
    public ReturnRequestResponseDTO rejectReturn(String vendorEmail, Long returnRequestId, String resolutionNote) {

        ReturnRequest request = returnRequestRepository
                .findByIdAndOrderItem_Product_Vendor_User_Email(returnRequestId, vendorEmail)
                .orElseThrow(() -> new SecurityException(
                        "This return request doesn't exist or doesn't belong to your account."));

        if (request.getStatus() != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("This return request has already been " + request.getStatus() + ".");
        }

        request.setStatus(ReturnStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolutionNote(resolutionNote);

        return mapToDTO(returnRequestRepository.save(request));
    }

    private ReturnRequestResponseDTO mapToDTO(ReturnRequest request) {
        ReturnRequestResponseDTO dto = new ReturnRequestResponseDTO();
        OrderItem item = request.getOrderItem();

        dto.setId(request.getId());
        dto.setOrderItemId(item.getId());
        dto.setOrderId(item.getOrder().getId());
        dto.setProductName(item.getProductName());
        dto.setLineTotal(item.getLineTotal());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus().name());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setResolvedAt(request.getResolvedAt());
        dto.setResolutionNote(request.getResolutionNote());

        refundRepository.findByOrderItemId(item.getId()).ifPresent(refund -> {
            RefundResponseDTO refundDto = new RefundResponseDTO();
            refundDto.setAmount(refund.getAmount());
            refundDto.setMethod(refund.getMethod());
            refundDto.setStatus(refund.getStatus().name());
            refundDto.setGatewayRefundId(refund.getGatewayRefundId());
            refundDto.setProcessedAt(refund.getProcessedAt());
            refundDto.setFailureReason(refund.getFailureReason());
            dto.setRefund(refundDto);
        });

        return dto;
    }
}