package shopstack_backend.dto;

public class PlaceCodOrderRequest {

    private Long addressId;

    public PlaceCodOrderRequest() {}

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}