package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shopstack_backend.dto.OrderItemResponseDTO;
import shopstack_backend.dto.OrderResponseDTO;
import shopstack_backend.dto.PaymentResponseDTO;
import shopstack_backend.entity.*;
import shopstack_backend.repository.CartRepository;
import shopstack_backend.repository.OrderRepository;
import shopstack_backend.repository.PaymentRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Turns the logged-in customer's cart into a real Order:
    // 1. Re-validates every item's stock right now (it may have changed
    //    since it was added to the cart).
    // 2. Snapshots each item's name and finalPrice at time of purchase.
    // 3. Actually reduces stock for every item.
    // 4. Records the payment that was already verified as successful
    //    (Razorpay signature check happens in RazorpayPaymentService,
    //    BEFORE this method is ever called).
    // 5. Clears the cart.
    // All in one transaction — if any item fails validation, nothing is
    // committed and nothing is charged/reduced.
    @Transactional
    public OrderResponseDTO checkout(String email, String paymentMethod, String transactionId) {

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalStateException("Your cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate every item BEFORE changing anything, so a failure partway
        // through never leaves stock partially reduced.
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (available < cartItem.getQuantity()) {
                throw new IllegalStateException(
                        "\"" + product.getName() + "\" only has " + available + " unit(s) left. " +
                        "Please update your cart before checking out.");
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            double finalPrice = product.getFinalPrice();
            double lineTotal = Math.round(finalPrice * cartItem.getQuantity() * 100.0) / 100.0;

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPriceAtPurchase(finalPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(lineTotal);
            orderItems.add(orderItem);

            total += lineTotal;

            // Actually reduce stock now that we know this order will commit.
            product.setStockQuantity(available(product) - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setTotalAmount(Math.round(total * 100.0) / 100.0);

        Order saved = orderRepository.save(order);

        Payment payment = recordPayment(saved, paymentMethod, transactionId);

        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);

        // Clear the cart now that everything's been turned into a paid order.
        cart.getItems().clear();
        cartRepository.save(cart);

        return mapToDTO(saved, payment);
    }

    // Kept for quick local testing without going through Razorpay at all
    // (e.g. automated tests, or a demo where you don't want to click
    // through the payment modal every time).
    @Transactional
    public OrderResponseDTO checkout(String email) {
        return checkout(email, "SIMULATED", "TXN-" + UUID.randomUUID());
    }

    private Payment recordPayment(Order order, String method, String transactionId) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId);
        payment.setAmount(order.getTotalAmount());
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private int available(Product product) {
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrderHistory(String email) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(order -> mapToDTO(order, paymentRepository.findByOrderId(order.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId, String email) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, email)
                .orElseThrow(() -> new SecurityException("Order not found for this account."));
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return mapToDTO(order, payment);
    }

    private OrderResponseDTO mapToDTO(Order order, Payment payment) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream().map(item -> {
            OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
            itemDto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
            itemDto.setProductName(item.getProductName());
            itemDto.setPriceAtPurchase(item.getPriceAtPurchase());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setLineTotal(item.getLineTotal());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);

        if (payment != null) {
            PaymentResponseDTO paymentDto = new PaymentResponseDTO();
            paymentDto.setMethod(payment.getMethod());
            paymentDto.setStatus(payment.getStatus().name());
            paymentDto.setTransactionId(payment.getTransactionId());
            paymentDto.setAmount(payment.getAmount());
            paymentDto.setPaidAt(payment.getPaidAt());
            dto.setPayment(paymentDto);
        }

        return dto;
    }
}