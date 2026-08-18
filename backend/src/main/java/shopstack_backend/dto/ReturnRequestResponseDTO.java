package shopstack_backend.dto;

import java.time.LocalDateTime;

public class ReturnRequestResponseDTO {

    private Long id;
    private Long orderItemId;
    private Long orderId;
    private String productName;
    private Double lineTotal;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private RefundResponseDTO refund; // null until the return is approved

    public ReturnRequestResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getLineTotal() { return lineTotal; }
    public void setLineTotal(Double lineTotal) { this.lineTotal = lineTotal; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public RefundResponseDTO getRefund() { return refund; }
    public void setRefund(RefundResponseDTO refund) { this.refund = refund; }
}