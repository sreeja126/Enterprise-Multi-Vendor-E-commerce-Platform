package shopstack_backend.dto;

public class BuyNowRequest {

    private Long productId;
    private Integer quantity;
    private Long addressId; // used only by the COD path

    public BuyNowRequest() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
}