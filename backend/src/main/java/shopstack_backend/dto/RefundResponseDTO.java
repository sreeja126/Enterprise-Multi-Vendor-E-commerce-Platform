package shopstack_backend.dto;

import java.time.LocalDateTime;

public class RefundResponseDTO {

    private Double amount;
    private String method;
    private String status;
    private String gatewayRefundId;
    private LocalDateTime processedAt;
    private String failureReason;

    public RefundResponseDTO() {}

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGatewayRefundId() { return gatewayRefundId; }
    public void setGatewayRefundId(String gatewayRefundId) { this.gatewayRefundId = gatewayRefundId; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}