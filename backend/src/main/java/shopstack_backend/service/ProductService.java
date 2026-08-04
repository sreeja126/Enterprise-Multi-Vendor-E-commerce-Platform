package shopstack_backend.service;

import shopstack_backend.dto.ProductRequestDTO;
import shopstack_backend.dto.ProductResponseDTO;
import shopstack_backend.entity.Category;
import shopstack_backend.entity.Product;
import shopstack_backend.entity.Vendor;
import shopstack_backend.repository.CategoryRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired(required = false)
    private CategoryRepository categoryRepository;

    @Autowired(required = false)
    private VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        return products.stream()
                .filter(Objects::nonNull)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findAll().stream()
                .filter(p -> p != null && p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        String lowerQuery = query.toLowerCase();
        return productRepository.findAll().stream()
                .filter(p -> p != null && (
                    (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery))
                ))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponseDTO addProduct(ProductRequestDTO dto) {
        Product product = new Product();
        mapDTOToProduct(dto, product);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        mapDTOToProduct(dto, product);
        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponseDTO setStock(Long productId, Integer absoluteStock) {
        if (absoluteStock == null || absoluteStock < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        product.setStockQuantity(absoluteStock);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Transactional
    public ProductResponseDTO processOrderDeduction(Long productId, Integer orderQuantity) {
        if (orderQuantity == null || orderQuantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than zero.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (currentStock < orderQuantity) {
            throw new IllegalStateException("Order placement failed: Product is Out of Stock or has insufficient quantity.");
        }

        product.setStockQuantity(currentStock - orderQuantity);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    // Helper: Maps DTO to Entity
    private void mapDTOToProduct(ProductRequestDTO dto, Product product) {
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice().doubleValue());
        if (dto.getStockQuantity() != null) product.setStockQuantity(dto.getStockQuantity());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());

        // Category Lookup
        if (dto.getCategoryId() != null && categoryRepository != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            product.setCategory(category);
        }

        // ✅ FIXED: Vendor Lookup using VendorRepository
        if (dto.getVendorId() != null && vendorRepository != null) {
            Vendor vendor = vendorRepository.findById(dto.getVendorId()).orElse(null);
            product.setVendor(vendor);
        }
    }

    // Helper: Maps Entity to DTO
    private ProductResponseDTO mapToDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        if (product == null) {
            return dto;
        }

        dto.setId(product.getId());
        dto.setName(product.getName() != null ? product.getName() : "");
        dto.setBrand(product.getBrand() != null ? product.getBrand() : "");
        dto.setDescription(product.getDescription() != null ? product.getDescription() : "");
        dto.setPrice(product.getPrice() != null ? product.getPrice() : 0.0);

        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        dto.setStock(stock);

        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            dto.setImages(Collections.singletonList(product.getImageUrl()));
        } else {
            dto.setImages(new ArrayList<>());
        }

        try {
            if (product.getCategory() != null) {
                dto.setCategoryName(product.getCategory().getName());
            } else {
                dto.setCategoryName("General");
            }
        } catch (Exception e) {
            dto.setCategoryName("General");
        }

        return dto;
    }
}