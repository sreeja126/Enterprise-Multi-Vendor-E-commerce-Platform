package shopstack_backend.dto;

import java.time.LocalDateTime;

public class VendorOrderItemResponseDTO {

    private Long id;              // order item id — target of the status update
    private Long orderId;
    private LocalDateTime orderCreatedAt;

    private String productName;
    private Double priceAtPurchase;
    private Integer quantity;
    private Double lineTotal;
    private String status;

    // Enough shipping info for the vendor to actually fulfill the item,
    // without exposing the customer's full account details.
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;

    public VendorOrderItemResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public LocalDateTime getOrderCreatedAt() { return orderCreatedAt; }
    public void setOrderCreatedAt(LocalDateTime orderCreatedAt) { this.orderCreatedAt = orderCreatedAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(Double priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getLineTotal() { return lineTotal; }
    public void setLineTotal(Double lineTotal) { this.lineTotal = lineTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShippingFullName() { return shippingFullName; }
    public void setShippingFullName(String shippingFullName) { this.shippingFullName = shippingFullName; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getShippingAddressLine1() { return shippingAddressLine1; }
    public void setShippingAddressLine1(String shippingAddressLine1) { this.shippingAddressLine1 = shippingAddressLine1; }

    public String getShippingAddressLine2() { return shippingAddressLine2; }
    public void setShippingAddressLine2(String shippingAddressLine2) { this.shippingAddressLine2 = shippingAddressLine2; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingState() { return shippingState; }
    public void setShippingState(String shippingState) { this.shippingState = shippingState; }

    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }

    public String getShippingCountry() { return shippingCountry; }
    public void setShippingCountry(String shippingCountry) { this.shippingCountry = shippingCountry; }
}