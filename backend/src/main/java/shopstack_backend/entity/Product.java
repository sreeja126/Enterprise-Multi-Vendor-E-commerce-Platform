package shopstack_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String brand;

    @Column(length = 1000)
    private String description;

    private Double price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    private String imageUrl;

    // Percentage off, 0-100. Null/0 means no discount is active.
    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Added missing vendor property mapped by Vendor.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    public Product() {}

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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity != null ? stockQuantity : 0;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getDiscountPercentage() {
        return discountPercentage != null ? discountPercentage : 0.0;
    }

    public void setDiscountPercentage(Double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    // The price the customer actually pays. Computed, not stored — so it's
    // always consistent with price/discountPercentage and can never drift
    // out of sync in the database.
    public Double getFinalPrice() {
        if (price == null) {
            return 0.0;
        }
        double discount = getDiscountPercentage();
        if (discount <= 0) {
            return price;
        }
        double final_ = price - (price * discount / 100.0);
        return Math.round(final_ * 100.0) / 100.0;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }
}