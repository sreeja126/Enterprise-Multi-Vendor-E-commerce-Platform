package shopstack_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import shopstack_backend.dto.ProductResponseDTO;
import shopstack_backend.entity.Product;
import shopstack_backend.service.ProductService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Add Product
   @PostMapping
public Product addProduct(
        Authentication authentication,
        @RequestBody Product product) {

    System.out.println("AUTH = " + authentication);

    return productService.addProduct(
            product,
            authentication != null ? authentication.getName() : "TEST"
    );
}
    // Get All Products
 @GetMapping
public List<ProductResponseDTO> getAllProducts(){

    return productService.getAllProducts();
}
    @GetMapping("/test")
public ResponseEntity<String> test() {
    return ResponseEntity.ok("Product API working");
}

    // Get Product By Id
    @GetMapping("/{id}")
    public ProductResponseDTO getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // Update Product
   @PutMapping("/{id}")
public Product updateProduct(
        @PathVariable Long id,
        @RequestBody Product product,
        Authentication authentication) {

    String email = authentication.getName();

    return productService.updateProduct(id, product, email);
}

    // Delete Product
   @DeleteMapping("/{id}")
public String deleteProduct(
        @PathVariable Long id,
        Authentication authentication) {

    String email = authentication.getName();

    productService.deleteProduct(id, email);

    return "Product deleted successfully";
}
    // Get Logged-in Vendor Products
    @GetMapping("/vendor")
    public List<ProductResponseDTO> getVendorProducts(Authentication authentication) {

        return productService.getProductsByVendor(
                authentication.getName());
    }

    // Get Products By Category
    @GetMapping("/category/{categoryId}")
public List<ProductResponseDTO> getByCategory(
        @PathVariable Long categoryId) {

    return productService.getProductsByCategory(categoryId);
}
    // Search Products
    @GetMapping("/search")
    public List<ProductResponseDTO> searchProducts(
            @RequestParam String keyword) {

        return productService.searchProducts(keyword);
    }
}