package shopstack_backend.controller;

import shopstack_backend.dto.ProductRequestDTO;
import shopstack_backend.dto.ProductResponseDTO;
import shopstack_backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(@RequestParam("query") String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    // Vendor is derived from the logged-in user (authentication.getName()),
    // never from the request body — see ProductService.addProduct.
    @PostMapping
    public ResponseEntity<?> addProduct(Authentication authentication,
                                         @RequestBody ProductRequestDTO productDTO) {
        try {
            ProductResponseDTO created = productService.addProduct(productDTO, authentication.getName());
            return ResponseEntity.ok(created);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Only the vendor who owns this product can update it.
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                            Authentication authentication,
                                            @RequestBody ProductRequestDTO productDTO) {
        try {
            ProductResponseDTO updated = productService.updateProduct(id, productDTO, authentication.getName());
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Only the vendor who owns this product can delete it.
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, Authentication authentication) {
        try {
            productService.deleteProduct(id, authentication.getName());
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Only the vendor who owns this product can set its stock directly.
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> setStockQuantity(@PathVariable Long id,
                                               Authentication authentication,
                                               @RequestBody Map<String, Integer> request) {
        try {
            Integer stockQuantity = request.get("stockQuantity");
            if (stockQuantity == null) {
                return ResponseEntity.badRequest().body("Field 'stockQuantity' is required.");
            }
            ProductResponseDTO updated = productService.setStock(id, stockQuantity, authentication.getName());
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NOT ownership-checked on purpose — this is the seam a future Order
    // module calls after checkout, for whichever vendor's product was
    // bought, regardless of who's logged in.
    @PostMapping("/{id}/reduce-stock")
    public ResponseEntity<?> reduceStockForOrder(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer quantity = request.get("quantity");
            if (quantity == null || quantity <= 0) {
                return ResponseEntity.badRequest().body("Valid purchase 'quantity' is required.");
            }
            ProductResponseDTO updated = productService.processOrderDeduction(id, quantity);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}