package shopstack_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String name;


    @Column(length = 1000)
    private String description;


    @Column(nullable = false)
    private Double price;


    @Column(nullable = false)
    private Integer stock;


    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;


    @Column(nullable = false)
    private String brand;


    @ElementCollection
    @CollectionTable(
        name = "product_images",
        joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_url")
    private List<String> images;


    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private User vendor;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    public Product() {
    }


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }


    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    // Getters

    public Long getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }


    public Double getPrice() {
        return price;
    }


    public Integer getStock() {
        return stock;
    }


    public String getBrand() {
        return brand;
    }


    public Category getCategory() {
        return category;
    }


    public List<String> getImages() {
        return images;
    }


    public User getVendor() {
        return vendor;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }



    // Setters


    public void setId(Long id) {
        this.id = id;
    }


    public void setName(String name) {
        this.name = name;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public void setPrice(Double price) {
        this.price = price;
    }


    public void setStock(Integer stock) {
        this.stock = stock;
    }


    public void setBrand(String brand) {
        this.brand = brand;
    }


    public void setCategory(Category category) {
        this.category = category;
    }


    public void setImages(List<String> images) {
        this.images = images;
    }


    public void setVendor(User vendor) {
        this.vendor = vendor;
    }


    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}