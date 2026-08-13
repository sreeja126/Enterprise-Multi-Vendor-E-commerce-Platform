package shopstack_backend.service;

import shopstack_backend.dto.ProductRequestDTO;
import shopstack_backend.dto.ProductResponseDTO;
import shopstack_backend.dto.VendorDTO;
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

    // Only this vendor's own products — this is what the Inventory /
    // My Products page must call, never getAllProducts().
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getMyProducts(String vendorEmail) {
        if (vendorRepository == null) {
            throw new RuntimeException("Vendor lookup is not configured on this server");
        }

        Vendor vendor = vendorRepository.findByEmail(vendorEmail)
                .orElseThrow(() -> new RuntimeException(
                        "No vendor profile found for this account."));

        return productRepository.findByVendor(vendor)
                .stream()
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

    // Vendor is always resolved server-side from the logged-in user's email
    // (the JWT subject) — never trusted from the request body. This is what
    // stops one vendor from creating a product under another vendor's name.
    @Transactional
    public ProductResponseDTO addProduct(ProductRequestDTO dto, String vendorEmail) {
        if (vendorRepository == null) {
            throw new RuntimeException("Vendor lookup is not configured on this server");
        }

        Vendor vendor = vendorRepository.findByEmail(vendorEmail)
                .orElseThrow(() -> new RuntimeException(
                        "No vendor profile found for this account. Register as a vendor first."));

        Product product = new Product();
        mapDTOToProduct(dto, product);
        product.setVendor(vendor);

        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto, String vendorEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        assertOwnership(product, vendorEmail);

        mapDTOToProduct(dto, product);
        Product updated = productRepository.save(product);
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteProduct(Long id, String vendorEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        assertOwnership(product, vendorEmail);

        productRepository.delete(product);
    }

    // Confirms the logged-in user is the vendor who owns this product.
    // Throws SecurityException (mapped to 403 in the controller) if not.
    private void assertOwnership(Product product, String vendorEmail) {
        Vendor vendor = product.getVendor();

        boolean owns = vendor != null
                && vendor.getUser() != null
                && vendor.getUser().getEmail() != null
                && vendor.getUser().getEmail().equalsIgnoreCase(vendorEmail);

        if (!owns) {
            throw new SecurityException("You do not have permission to modify this product.");
        }
    }

    @Transactional
    public ProductResponseDTO setStock(Long productId, Integer absoluteStock, String vendorEmail) {
        if (absoluteStock == null || absoluteStock < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        assertOwnership(product, vendorEmail);

        product.setStockQuantity(absoluteStock);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    // Apply (or clear, by passing 0) a discount percentage on a product.
    // Ownership-checked the same way as setStock — a vendor can only
    // discount their own products.
    @Transactional
    public ProductResponseDTO setDiscount(Long productId, Double discountPercentage, String vendorEmail) {
        if (discountPercentage == null || discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        assertOwnership(product, vendorEmail);

        product.setDiscountPercentage(discountPercentage);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }

    // Intentionally NOT ownership-checked — this is what a future Order
    // module will call after checkout for ANY vendor's product, so it must
    // stay callable regardless of who's logged in (or even system-triggered
    // with no logged-in user at all).
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

    // Helper: Maps DTO to Entity. Vendor is deliberately NOT set here
    // anymore — addProduct sets it from the authenticated user, and
    // updateProduct never changes a product's owning vendor at all.
    private void mapDTOToProduct(ProductRequestDTO dto, Product product) {
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice().doubleValue());
        if (dto.getStockQuantity() != null) product.setStockQuantity(dto.getStockQuantity());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());

        if (dto.getCategoryId() != null && categoryRepository != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            product.setCategory(category);
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
        dto.setDiscountPercentage(product.getDiscountPercentage());
        dto.setFinalPrice(product.getFinalPrice());

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

        try {
            Vendor vendor = product.getVendor();
            if (vendor != null) {
                dto.setVendor(new VendorDTO(vendor.getId(), vendor.getName(), vendor.getEmail()));
            }
        } catch (Exception e) {
            // Leave vendor null rather than fail the whole response.
        }

        return dto;
    }
}