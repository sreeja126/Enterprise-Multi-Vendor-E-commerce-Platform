package shopstack_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One return request per order item — an item that's already been
    // requested/resolved shouldn't get a second concurrent request.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime resolvedAt;

    // Vendor's note when approving/rejecting — e.g. why a return was denied.
    @Column(length = 1000)
    private String resolutionNote;

    public ReturnRequest() {}

    @PrePersist
    public void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReturnStatus.REQUESTED;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}