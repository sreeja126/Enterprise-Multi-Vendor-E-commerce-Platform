package shopstack_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import shopstack_backend.dto.ProductResponseDTO;
import shopstack_backend.dto.VendorDTO;
import shopstack_backend.entity.Product;
import shopstack_backend.entity.User;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.UserRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository,
                          UserRepository userRepository) {

        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }


    // Add Product
    public Product addProduct(Product product, String email) {

        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        product.setVendor(vendor);

        return productRepository.save(product);
    }


    // Get All Products
    public List<ProductResponseDTO> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::convert)
                .toList();
    }


    // Get Product By Id
    public ProductResponseDTO getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        return convert(product);
    }


    // Update Product
    public Product updateProduct(Long id,
                                 Product updatedProduct,
                                 String email) {

        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found"));


        Product product = productRepository
                .findByIdAndVendor(id, vendor)
                .orElseThrow(() ->
                        new RuntimeException("Unauthorized"));


        product.setName(updatedProduct.getName());
        product.setBrand(updatedProduct.getBrand());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setCategory(updatedProduct.getCategory());
        product.setImages(updatedProduct.getImages());


        return productRepository.save(product);
    }


    // Delete Product
    public void deleteProduct(Long id, String email) {

        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found"));


        Product product = productRepository
                .findByIdAndVendor(id, vendor)
                .orElseThrow(() ->
                        new RuntimeException("Unauthorized"));


        productRepository.delete(product);
    }


    // Vendor Products
    public List<ProductResponseDTO> getProductsByVendor(String email) {

        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found"));


        return productRepository.findByVendor(vendor)
                .stream()
                .map(this::convert)
                .toList();
    }



    // Products By Category
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {

        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::convert)
                .toList();
    }



    // Search Products
    public List<ProductResponseDTO> searchProducts(String keyword) {

        return productRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::convert)
                .toList();
    }



    // Convert Entity -> DTO
    public ProductResponseDTO convert(Product product) {

        ProductResponseDTO dto = new ProductResponseDTO();


        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());

        dto.setBrand(product.getBrand());
        dto.setDescription(product.getDescription());
        dto.setStock(product.getStock());


        // Category
        if(product.getCategory() != null) {

            dto.setCategoryName(
                    product.getCategory().getName()
            );
        }


        // Images
        dto.setImages(product.getImages());


        // Vendor details
        User vendor = product.getVendor();

        if(vendor != null) {

            dto.setVendor(
                    new VendorDTO(
                            vendor.getId(),
                            vendor.getFullName(),
                            vendor.getEmail()
                    )
            );
        }


        return dto;
    }
}