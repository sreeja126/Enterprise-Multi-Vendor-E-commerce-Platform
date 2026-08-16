package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shopstack_backend.dto.OrderItemResponseDTO;
import shopstack_backend.dto.OrderResponseDTO;
import shopstack_backend.dto.PaymentResponseDTO;
import shopstack_backend.dto.VendorOrderItemResponseDTO;
import shopstack_backend.entity.*;
import shopstack_backend.repository.AddressRepository;
import shopstack_backend.repository.CartRepository;
import shopstack_backend.repository.OrderItemRepository;
import shopstack_backend.repository.OrderRepository;
import shopstack_backend.repository.PaymentRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AddressRepository addressRepository;

    // The standard forward progression. CANCELLED/RETURNED/REFUNDED are
    // exceptions handled separately below, not part of this sequence.
    private static final List<OrderStatus> PROGRESSION = Arrays.asList(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private static final Set<OrderStatus> TERMINAL_STATUSES = Set.of(
            OrderStatus.CANCELLED, OrderStatus.RETURNED, OrderStatus.REFUNDED
    );

    // Turns the logged-in customer's cart into a real Order.
    @Transactional
    public OrderResponseDTO checkout(String email, Long addressId, String paymentMethod, String transactionId) {

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalStateException("Your cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = addressRepository.findByIdAndUserEmail(addressId, email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Please select a valid shipping address before checking out."));

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

        order.setShippingFullName(address.getFullName());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());

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
            // Each item starts CONFIRMED — the order as a whole is only
            // "confirmed" once payment succeeds, which is exactly when
            // this method runs.
            orderItem.setStatus(OrderStatus.CONFIRMED);
            orderItems.add(orderItem);

            total += lineTotal;

            product.setStockQuantity(available(product) - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setTotalAmount(Math.round(total * 100.0) / 100.0);

        Order saved = orderRepository.save(order);

        Payment payment = recordPayment(saved, paymentMethod, transactionId);

        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);

        cart.getItems().clear();
        cartRepository.save(cart);

        // Calculate customer's order number for response
        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        OrderResponseDTO dto = mapToDTO(saved, payment);
        dto.setCustomerOrderNumber(userOrders.size());

        return dto;
    }

    @Transactional
    public OrderResponseDTO checkout(String email) {
        Address defaultAddress = addressRepository.findByUserEmailAndIsDefaultTrue(email)
                .orElseThrow(() -> new IllegalStateException(
                        "No default address on file. Add a shipping address first."));
        return checkout(email, defaultAddress.getId(), "SIMULATED", "TXN-" + UUID.randomUUID());
    }

    // Cash on Delivery — a real order gets created just like an online
    // payment does (stock reduced, cart cleared, address snapshotted), but
    // the Payment record is PENDING instead of SUCCESS, since no money has
    // actually changed hands yet.
    @Transactional
    public OrderResponseDTO placeCodOrder(String email, Long addressId) {
        return checkoutWithStatus(email, addressId, "COD", "COD-" + UUID.randomUUID(), PaymentStatus.PENDING);
    }

    // Internal variant of checkout() that lets the caller specify the
    // Payment's status explicitly (SUCCESS for Razorpay, PENDING for COD).
    private OrderResponseDTO checkoutWithStatus(String email, Long addressId, String paymentMethod,
                                                 String transactionId, PaymentStatus paymentStatus) {

        Cart cart = cartRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalStateException("Your cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = addressRepository.findByIdAndUserEmail(addressId, email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Please select a valid shipping address before checking out."));

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

        order.setShippingFullName(address.getFullName());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());

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
            orderItem.setStatus(OrderStatus.CONFIRMED);
            orderItems.add(orderItem);

            total += lineTotal;

            product.setStockQuantity(available(product) - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setTotalAmount(Math.round(total * 100.0) / 100.0);

        Order saved = orderRepository.save(order);

        Payment payment = recordPayment(saved, paymentMethod, transactionId, paymentStatus);

        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);

        cart.getItems().clear();
        cartRepository.save(cart);

        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        OrderResponseDTO dto = mapToDTO(saved, payment);
        dto.setCustomerOrderNumber(userOrders.size());

        return dto;
    }

    // ---- Buy Now: an express checkout for ONE product, completely
    // independent of the customer's cart. This never reads or writes
    // Cart/CartItem at all.
    @Transactional
    public OrderResponseDTO checkoutSingleItem(String email, Long addressId, Long productId, Integer quantity,
                                                String paymentMethod, String transactionId, PaymentStatus paymentStatus) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = addressRepository.findByIdAndUserEmail(addressId, email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Please select a valid shipping address before checking out."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (available < quantity) {
            throw new IllegalStateException(
                    "\"" + product.getName() + "\" only has " + available + " unit(s) left.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        order.setShippingFullName(address.getFullName());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());

        double finalPrice = product.getFinalPrice();
        double lineTotal = Math.round(finalPrice * quantity * 100.0) / 100.0;

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setProductName(product.getName());
        orderItem.setPriceAtPurchase(finalPrice);
        orderItem.setQuantity(quantity);
        orderItem.setLineTotal(lineTotal);
        orderItem.setStatus(OrderStatus.CONFIRMED);

        order.setItems(new ArrayList<>(List.of(orderItem)));
        order.setTotalAmount(lineTotal);

        Order saved = orderRepository.save(order);

        Payment payment = recordPayment(saved, paymentMethod, transactionId, paymentStatus);

        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);

        // Reduce stock for just this product — the cart is never touched.
        product.setStockQuantity(available - quantity);
        productRepository.save(product);

        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        OrderResponseDTO dto = mapToDTO(saved, payment);
        dto.setCustomerOrderNumber(userOrders.size());

        return dto;
    }

    @Transactional
    public OrderResponseDTO placeCodBuyNowOrder(String email, Long addressId, Long productId, Integer quantity) {
        return checkoutSingleItem(email, addressId, productId, quantity,
                "COD", "COD-" + UUID.randomUUID(), PaymentStatus.PENDING);
    }

    private Payment recordPayment(Order order, String method, String transactionId) {
        return recordPayment(order, method, transactionId, PaymentStatus.SUCCESS);
    }

    private Payment recordPayment(Order order, String method, String transactionId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setStatus(status);
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
        // 1. Fetch user's orders in ascending creation order (1st order, 2nd order, etc.)
        List<Order> orders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        List<OrderResponseDTO> dtoList = new ArrayList<>();

        // 2. Assign sequence numbers per customer
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

            OrderResponseDTO dto = mapToDTO(order, payment);
            dto.setCustomerOrderNumber(i + 1); // 1-indexed count per user

            dtoList.add(dto);
        }

        // 3. Reverse list so newest orders appear at the top in UI
        Collections.reverse(dtoList);

        return dtoList;
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId, String email) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, email)
                .orElseThrow(() -> new SecurityException("Order not found for this account."));
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        
        OrderResponseDTO dto = mapToDTO(order, payment);

        // Find customer sequence number for single order lookup
        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        for (int i = 0; i < userOrders.size(); i++) {
            if (userOrders.get(i).getId().equals(order.getId())) {
                dto.setCustomerOrderNumber(i + 1);
                break;
            }
        }

        return dto;
    }

    // ---- Vendor-facing order fulfillment ----

    // Only this vendor's own line items, across every order they appear
    // in — never another vendor's items, even from the same order.
    @Transactional(readOnly = true)
    public List<VendorOrderItemResponseDTO> getVendorOrderItems(String vendorEmail) {
        return orderItemRepository
                .findByProduct_Vendor_User_EmailOrderByOrder_CreatedAtDesc(vendorEmail)
                .stream()
                .map(this::mapToVendorDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VendorOrderItemResponseDTO updateItemStatus(String vendorEmail, Long orderItemId, String newStatusRaw) {

        OrderItem item = orderItemRepository.findByIdAndProduct_Vendor_User_Email(orderItemId, vendorEmail)
                .orElseThrow(() -> new SecurityException(
                        "This order item doesn't exist or doesn't belong to your account."));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusRaw.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("\"" + newStatusRaw + "\" is not a valid order status.");
        }

        OrderStatus currentStatus = item.getStatus();

        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException(
                    "This item is already " + currentStatus + " and can't be changed further.");
        }

        // CANCELLED is always allowed from a non-terminal state (vendor
        // needs an escape hatch — e.g. out of stock after all). Everything
        // else must move strictly forward through the standard progression.
        if (newStatus != OrderStatus.CANCELLED) {
            int currentIndex = PROGRESSION.indexOf(currentStatus);
            int newIndex = PROGRESSION.indexOf(newStatus);

            if (newIndex == -1) {
                throw new IllegalArgumentException(
                        newStatus + " can't be set directly — use CANCELLED to stop an order, " +
                        "or progress it through PROCESSING → SHIPPED → DELIVERED.");
            }

            if (newIndex <= currentIndex) {
                throw new IllegalStateException(
                        "Can't move from " + currentStatus + " back to " + newStatus + ". " +
                        "Status can only move forward.");
            }
        }

        item.setStatus(newStatus);
        orderItemRepository.save(item);

        recomputeOrderStatus(item.getOrder());

        return mapToVendorDTO(item);
    }

    // ---- Customer-initiated cancellation ----

    // A customer can cancel their own order item only while it's still
    // PENDING or CONFIRMED — once a vendor has moved it to PROCESSING or
    // beyond, they're already acting on it, so the customer can no longer
    // unilaterally back out (a real Return request is the right path at
    // that point, once that module exists).
    @Transactional
    public OrderResponseDTO cancelOrderItem(String customerEmail, Long orderItemId) {

        OrderItem item = orderItemRepository.findByIdAndOrder_User_Email(orderItemId, customerEmail)
                .orElseThrow(() -> new SecurityException(
                        "This order item doesn't exist or doesn't belong to your account."));

        OrderStatus currentStatus = item.getStatus();

        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException("This item is already " + currentStatus + ".");
        }

        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "This item is already " + currentStatus + " and can no longer be cancelled. " +
                    "Contact the seller if you need to return it once delivered.");
        }

        // Give the stock back — it was reduced at checkout time.
        Product product = item.getProduct();
        if (product != null) {
            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            product.setStockQuantity(currentStock + item.getQuantity());
            productRepository.save(product);
        }

        item.setStatus(OrderStatus.CANCELLED);
        orderItemRepository.save(item);

        Order order = item.getOrder();
        recomputeOrderStatus(order);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return mapToDTO(order, payment);
    }

    // Cancels every still-cancellable item on an order in one action —
    // used for "Cancel Order" rather than cancelling line items one at a time.
    // Items already past CONFIRMED are simply left alone rather than
    // failing the whole request.
    @Transactional
    public OrderResponseDTO cancelOrder(String customerEmail, Long orderId) {

        Order order = orderRepository.findByIdAndUserEmail(orderId, customerEmail)
                .orElseThrow(() -> new SecurityException("Order not found for this account."));

        boolean cancelledAny = false;

        for (OrderItem item : order.getItems()) {
            OrderStatus status = item.getStatus();
            if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
                Product product = item.getProduct();
                if (product != null) {
                    int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                    product.setStockQuantity(currentStock + item.getQuantity());
                    productRepository.save(product);
                }
                item.setStatus(OrderStatus.CANCELLED);
                orderItemRepository.save(item);
                cancelledAny = true;
            }
        }

        if (!cancelledAny) {
            throw new IllegalStateException(
                    "None of the items in this order can be cancelled anymore — they're already being processed.");
        }

        recomputeOrderStatus(order);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return mapToDTO(order, payment);
    }

    // The parent Order's overall status is the LEAST advanced status among
    // its items — an order isn't "Shipped" to the customer until every
    // vendor's part of it has shipped. A CANCELLED item doesn't hold the
    // whole order back if other items are still progressing normally.
    private void recomputeOrderStatus(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) return;

        OrderStatus least = null;

        for (OrderItem item : items) {
            OrderStatus s = item.getStatus();
            if (s == OrderStatus.CANCELLED) continue; // don't let a cancelled item hold others back

            int idx = PROGRESSION.indexOf(s);
            if (idx == -1) continue; // RETURNED/REFUNDED items also excluded from this comparison

            if (least == null || idx < PROGRESSION.indexOf(least)) {
                least = s;
            }
        }

        // Every item was cancelled/returned/refunded — reflect that on the order.
        if (least == null) {
            boolean allCancelled = items.stream().allMatch(i -> i.getStatus() == OrderStatus.CANCELLED);
            order.setStatus(allCancelled ? OrderStatus.CANCELLED : order.getStatus());
        } else {
            order.setStatus(least);
        }

        orderRepository.save(order);
    }

    private VendorOrderItemResponseDTO mapToVendorDTO(OrderItem item) {
        VendorOrderItemResponseDTO dto = new VendorOrderItemResponseDTO();
        Order order = item.getOrder();

        dto.setId(item.getId());
        dto.setOrderId(order.getId());
        dto.setOrderCreatedAt(order.getCreatedAt());
        dto.setProductName(item.getProductName());
        dto.setPriceAtPurchase(item.getPriceAtPurchase());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getLineTotal());
        dto.setStatus(item.getStatus().name());

        dto.setShippingFullName(order.getShippingFullName());
        dto.setShippingPhone(order.getShippingPhone());
        dto.setShippingAddressLine1(order.getShippingAddressLine1());
        dto.setShippingAddressLine2(order.getShippingAddressLine2());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingPostalCode(order.getShippingPostalCode());
        dto.setShippingCountry(order.getShippingCountry());

        return dto;
    }

    private OrderResponseDTO mapToDTO(Order order, Payment payment) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());

        dto.setShippingFullName(order.getShippingFullName());
        dto.setShippingPhone(order.getShippingPhone());
        dto.setShippingAddressLine1(order.getShippingAddressLine1());
        dto.setShippingAddressLine2(order.getShippingAddressLine2());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingPostalCode(order.getShippingPostalCode());
        dto.setShippingCountry(order.getShippingCountry());

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream().map(item -> {
            OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
            itemDto.setProductName(item.getProductName());
            itemDto.setPriceAtPurchase(item.getPriceAtPurchase());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setLineTotal(item.getLineTotal());
            itemDto.setStatus(item.getStatus() != null ? item.getStatus().name() : null);
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