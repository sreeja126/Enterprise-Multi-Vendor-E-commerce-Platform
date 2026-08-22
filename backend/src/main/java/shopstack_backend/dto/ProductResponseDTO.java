package shopstack_backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductResponseDTO {

    private Long id;

    private String name;

    private String brand;

    private String description;

    private BigDecimal price;

    private double discountPercentage;

    private BigDecimal finalPrice;

    private int stock;

    private boolean isOutOfStock;

    private List<String> images;

    private String categoryName;

    private VendorDTO vendor;

    public ProductResponseDTO() {}

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }


    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }


    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }


    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
        this.isOutOfStock = (stock <= 0);
    }

    // Convenience aliases for stock quantity compatibility
    public int getStockQuantity() {
        return stock;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stock = stockQuantity;
        this.isOutOfStock = (stockQuantity <= 0);
    }

    public boolean isOutOfStock() {
        return isOutOfStock;
    }

    public void setOutOfStock(boolean outOfStock) {
        isOutOfStock = outOfStock;
    }


    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }


    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }


    public VendorDTO getVendor() {
        return vendor;
    }

    public void setVendor(VendorDTO vendor) {
        this.vendor = vendor;
    }
}