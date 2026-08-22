package shopstack_backend.service;

import shopstack_backend.dto.InventoryReportResponseDTO;
import shopstack_backend.dto.ProductRequestDTO;
import shopstack_backend.dto.ProductResponseDTO;
import shopstack_backend.dto.StockHistoryResponseDTO;
import shopstack_backend.dto.VendorDTO;
import shopstack_backend.entity.Category;
import shopstack_backend.entity.Product;
import shopstack_backend.entity.StockHistory;
import shopstack_backend.entity.Vendor;
import shopstack_backend.repository.CategoryRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.StockHistoryRepository;
import shopstack_backend.repository.VendorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {

    // Matches the threshold used for the Low Stock badge
    public static final int LOW_STOCK_THRESHOLD = 5;

    @Autowired
    private ProductRepository productRepository;

    @Autowired(required = false)
    private CategoryRepository categoryRepository;

    @Autowired(required = false)
    private VendorRepository vendorRepository;

    @Autowired(required = false)
    private StockHistoryRepository stockHistoryRepository;
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
    public List<ProductResponseDTO> getMyProducts(String vendorEmail) {

        if (vendorRepository == null) {
            throw new RuntimeException(
                    "Vendor lookup is not configured on this server");
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
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: " + id));

        return mapToDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {

        return productRepository.findAll()
                .stream()
                .filter(p ->
                        p != null
                                && p.getCategory() != null
                                && categoryId.equals(
                                        p.getCategory().getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String query) {

        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }

        String lowerQuery = query.toLowerCase();

        return productRepository.findAll()
                .stream()
                .filter(p ->
                        p != null && ( (p.getName() != null && p.getName().toLowerCase() .contains(lowerQuery))   || (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery))   ))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponseDTO addProduct(
            ProductRequestDTO dto,
            String vendorEmail) {

        if (vendorRepository == null) {
            throw new RuntimeException(
                    "Vendor lookup is not configured on this server");
        }

        Vendor vendor = vendorRepository.findByEmail(vendorEmail)
                .orElseThrow(() -> new RuntimeException(
                        "No vendor profile found for this account. "
                                + "Register as a vendor first."));

        Product product = new Product();

        mapDTOToProduct(dto, product);

        // Vendor is always taken from authenticated user
        product.setVendor(vendor);

        Product saved = productRepository.save(product);

        return mapToDTO(saved);
    }
    @Transactional
    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO dto,
            String vendorEmail) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: " + id));

        assertOwnership(product, vendorEmail);

        mapDTOToProduct(dto, product);

        Product updated = productRepository.save(product);

        return mapToDTO(updated);
    }
    @Transactional
    public void deleteProduct(
            Long id,
            String vendorEmail) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: " + id));

        assertOwnership(product, vendorEmail);

        productRepository.delete(product);
    }

    private void assertOwnership(
            Product product,
            String vendorEmail) {

        Vendor vendor = product.getVendor();

        boolean owns =
                vendor != null
                        && vendor.getUser() != null
                        && vendor.getUser().getEmail() != null
                        && vendor.getUser()
                        .getEmail()
                        .equalsIgnoreCase(vendorEmail);

        if (!owns) {
            throw new SecurityException(
                    "You do not have permission to modify this product.");
        }
    }

    @Transactional
    public ProductResponseDTO setStock(
            Long productId,
            Integer absoluteStock,
            String vendorEmail) {

        if (absoluteStock == null || absoluteStock < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: " + productId));

        assertOwnership(product, vendorEmail);

        int previousStock =
                product.getStockQuantity() != null
                        ? product.getStockQuantity()
                        : 0;

        product.setStockQuantity(absoluteStock);

        Product saved = productRepository.save(product);

        recordStockChange(
                saved,
                previousStock,
                absoluteStock,
                "Manual Update"
        );

        return mapToDTO(saved);
    }
    public void recordStockChange(
            Product product,
            int previousStock,
            int newStock,
            String reason) {

        if (stockHistoryRepository == null) {
            return;
        }

        if (previousStock == newStock) {
            return;
        }

        StockHistory history = new StockHistory();

        history.setProduct(product);
        history.setPreviousStock(previousStock);
        history.setNewStock(newStock);
        history.setReason(reason);

        stockHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<StockHistoryResponseDTO> getStockHistory(
            Long productId,
            String vendorEmail) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found for id: " + productId));

        assertOwnership(product, vendorEmail);

        if (stockHistoryRepository == null) {
            return new ArrayList<>();
        }

        return stockHistoryRepository
                .findByProductIdOrderByChangedAtDesc(productId)
                .stream()
                .map(this::mapHistoryToDTO)
                .collect(Collectors.toList());
    }
    private StockHistoryResponseDTO mapHistoryToDTO(
            StockHistory h) {

        StockHistoryResponseDTO dto =
                new StockHistoryResponseDTO();

        dto.setId(h.getId());
        dto.setProductId(h.getProduct().getId());
        dto.setProductName(h.getProduct().getName());
        dto.setPreviousStock(h.getPreviousStock());
        dto.setNewStock(h.getNewStock());
        dto.setReason(h.getReason());
        dto.setChangedAt(h.getChangedAt());

        return dto;
    }
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts(
            String vendorEmail) {

        List<ProductResponseDTO> myProducts =
                getMyProducts(vendorEmail);

        return myProducts.stream()
                .filter(p ->
                        p.getStock() <= LOW_STOCK_THRESHOLD)
                .collect(Collectors.toList());
    }
  @Transactional(readOnly = true)
    public InventoryReportResponseDTO getInventoryReport(
            String vendorEmail) {
        List<ProductResponseDTO> myProducts =
                getMyProducts(vendorEmail);

        InventoryReportResponseDTO report =
                new InventoryReportResponseDTO();
        report.setTotalProducts(myProducts.size());
        report.setLowStockThreshold(LOW_STOCK_THRESHOLD);
        int totalUnits = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        int outOfStock = 0;
        int lowStock = 0;
        for (ProductResponseDTO p : myProducts) {
            int stock = p.getStock();
            totalUnits += stock;
            BigDecimal finalPrice =
                    p.getFinalPrice() != null
                            ? p.getFinalPrice()
                            : BigDecimal.ZERO;
            BigDecimal stockValue =
                    finalPrice.multiply(
                            BigDecimal.valueOf(stock)
                    );
            totalValue = totalValue.add(stockValue);
            if (stock == 0) {
                outOfStock++;
            } else if (stock <= LOW_STOCK_THRESHOLD) {
                lowStock++;
            }
        }
        report.setTotalStockUnits(totalUnits);
        report.setTotalStockValue(
                totalValue.setScale( 2, RoundingMode.HALF_UP ) );
        report.setOutOfStockCount(outOfStock);
        report.setLowStockCount(lowStock);
        return report;
    }
    @Transactional
    public ProductResponseDTO setDiscount(
            Long productId,
            Double discountPercentage,
            String vendorEmail) {
        if (discountPercentage == null
                || discountPercentage < 0
                || discountPercentage > 100) {
            throw new IllegalArgumentException(
                    "Discount must be between 0 and 100.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: "
                                        + productId));
        assertOwnership(product, vendorEmail);
        product.setDiscountPercentage(discountPercentage);
        Product saved = productRepository.save(product);
        return mapToDTO(saved);
    }
    @Transactional
    public ProductResponseDTO processOrderDeduction(
            Long productId,
            Integer orderQuantity) {
        if (orderQuantity == null || orderQuantity <= 0) {
            throw new IllegalArgumentException(
                    "Order quantity must be greater than zero.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: "
                                        + productId));
        int currentStock =
                product.getStockQuantity() != null
                        ? product.getStockQuantity() : 0;
        if (currentStock < orderQuantity) {
            throw new IllegalStateException(
                    "Order placement failed: Product is Out of Stock "
                            + "or has insufficient quantity.");
        }

        product.setStockQuantity(
                currentStock - orderQuantity
        );

        Product saved =
                productRepository.save(product);

        return mapToDTO(saved);
    }
    private void mapDTOToProduct(
            ProductRequestDTO dto,
            Product product) {

        if (dto.getName() != null) {
            product.setName(dto.getName());
        }

        if (dto.getBrand() != null) {
            product.setBrand(dto.getBrand());
        }

        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
       if (dto.getStockQuantity() != null) {
            product.setStockQuantity(
                    dto.getStockQuantity());
        }
        if (dto.getImageUrl() != null) {
            product.setImageUrl(
                    dto.getImageUrl());
        }
        if (dto.getCategoryId() != null
                && categoryRepository != null) {

            Category category =
                    categoryRepository
                            .findById(dto.getCategoryId())
                            .orElse(null);

            product.setCategory(category);
        }
    }
   private ProductResponseDTO mapToDTO(
            Product product) {

        ProductResponseDTO dto =
                new ProductResponseDTO();

        if (product == null) {
            return dto;
        }
   dto.setId(product.getId());
        dto.setName(
                product.getName() != null
                        ? product.getName()
                        : ""
        );
        dto.setBrand(
                product.getBrand() != null
                        ? product.getBrand()
                        : ""
        );
        dto.setDescription(
                product.getDescription() != null
                        ? product.getDescription()
                        : ""
        );
        dto.setPrice(
                product.getPrice() != null
                        ? product.getPrice()
                        : BigDecimal.ZERO
        );
        dto.setDiscountPercentage(
                product.getDiscountPercentage()
        );
        dto.setFinalPrice(
                product.getFinalPrice()
        );
        int stock =
                product.getStockQuantity() != null
                        ? product.getStockQuantity()
                        : 0;

        dto.setStock(stock);
        if (product.getImageUrl() != null
                && !product.getImageUrl()
                .trim()
                .isEmpty()) {

            dto.setImages(
                    Collections.singletonList(
                            product.getImageUrl()
                    )
            );

        } else {

            dto.setImages(
                    new ArrayList<>()
            );
        }
        try {

            if (product.getCategory() != null) {
    dto.setCategoryName(
                        product.getCategory().getName()
                );

            } else {
   dto.setCategoryName("General");
            }
  } catch (Exception e) {
            dto.setCategoryName("General");
        }
        try {
            Vendor vendor =
                    product.getVendor();
            if (vendor != null) {
                dto.setVendor(
                        new VendorDTO(   vendor.getId(), vendor.getName(), vendor.getEmail()  ) );
            }
  } catch (Exception e) {}
        return dto;
    }
}