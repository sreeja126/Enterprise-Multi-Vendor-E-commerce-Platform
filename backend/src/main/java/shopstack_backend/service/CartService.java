package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shopstack_backend.dto.CartItemResponseDTO;
import shopstack_backend.dto.CartResponseDTO;
import shopstack_backend.entity.Cart;
import shopstack_backend.entity.CartItem;
import shopstack_backend.entity.Product;
import shopstack_backend.entity.User;
import shopstack_backend.repository.CartItemRepository;
import shopstack_backend.repository.CartRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // Get the logged-in customer's cart, creating an empty one if this is
    // their first time adding anything.
    @Transactional
    public Cart getOrCreateCart(String email) {
        return cartRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Transactional(readOnly = true)
    public CartResponseDTO getCart(String email) {
        Cart cart = cartRepository.findByUserEmail(email).orElse(null);
        if (cart == null) {
            return emptyCartResponse();
        }
        return mapToDTO(cart);
    }

    @Transactional
    public CartResponseDTO addItem(String email, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (available <= 0) {
            throw new IllegalStateException("This product is out of stock.");
        }

        Cart cart = getOrCreateCart(email);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        int requestedTotal = quantity + (item != null ? item.getQuantity() : 0);
        if (requestedTotal > available) {
            throw new IllegalStateException(
                    "Only " + available + " unit(s) of \"" + product.getName() + "\" available.");
        }

        if (item != null) {
            item.setQuantity(requestedTotal);
        } else {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }

        cartItemRepository.save(item);

        return mapToDTO(cartRepository.findByUserEmail(email).orElseThrow());
    }

    @Transactional
    public CartResponseDTO updateItemQuantity(String email, Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new SecurityException("Cart item not found for this account."));

        int available = item.getProduct().getStockQuantity() != null
                ? item.getProduct().getStockQuantity() : 0;

        if (quantity > available) {
            throw new IllegalStateException(
                    "Only " + available + " unit(s) of \"" + item.getProduct().getName() + "\" available.");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return mapToDTO(cartRepository.findByUserEmail(email).orElseThrow());
    }

    @Transactional
    public CartResponseDTO removeItem(String email, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserEmail(itemId, email)
                .orElseThrow(() -> new SecurityException("Cart item not found for this account."));

        cartItemRepository.delete(item);

        return mapToDTO(cartRepository.findByUserEmail(email).orElseThrow());
    }

    @Transactional
    public CartResponseDTO clearCart(String email) {
        Cart cart = cartRepository.findByUserEmail(email).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
        return emptyCartResponse();
    }

    private CartResponseDTO mapToDTO(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setCartId(cart.getId());

        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        double total = 0;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;

            CartItemResponseDTO itemDto = new CartItemResponseDTO();
            itemDto.setId(item.getId());
            itemDto.setProductId(product.getId());
            itemDto.setProductName(product.getName());
            itemDto.setImageUrl(product.getImageUrl());
            itemDto.setPrice(product.getPrice() != null ? product.getPrice() : 0.0);
            itemDto.setDiscountPercentage(product.getDiscountPercentage());

            // The whole point of this module: cart pricing always uses
            // finalPrice (post-discount), never the raw price.
            double finalPrice = product.getFinalPrice();
            itemDto.setFinalPrice(finalPrice);

            itemDto.setQuantity(item.getQuantity());
            itemDto.setAvailableStock(product.getStockQuantity() != null ? product.getStockQuantity() : 0);

            double lineTotal = Math.round(finalPrice * item.getQuantity() * 100.0) / 100.0;
            itemDto.setLineTotal(lineTotal);

            total += lineTotal;
            itemDTOs.add(itemDto);
        }

        dto.setItems(itemDTOs);
        dto.setTotalAmount(Math.round(total * 100.0) / 100.0);
        dto.setTotalItems(itemDTOs.stream().mapToInt(CartItemResponseDTO::getQuantity).sum());

        return dto;
    }

    private CartResponseDTO emptyCartResponse() {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setCartId(null);
        dto.setItems(new ArrayList<>());
        dto.setTotalAmount(0);
        dto.setTotalItems(0);
        return dto;
    }
}