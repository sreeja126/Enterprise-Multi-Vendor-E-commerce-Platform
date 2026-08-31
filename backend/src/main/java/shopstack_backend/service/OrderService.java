package shopstack_backend.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shopstack_backend.dto.OrderItemResponseDTO;
import shopstack_backend.dto.OrderResponseDTO;
import shopstack_backend.dto.PaymentResponseDTO;
import shopstack_backend.dto.RefundResponseDTO;
import shopstack_backend.dto.VendorOrderItemResponseDTO;
import shopstack_backend.entity.*;
import shopstack_backend.repository.AddressRepository;
import shopstack_backend.repository.CartRepository;
import shopstack_backend.repository.OrderItemRepository;
import shopstack_backend.repository.OrderRepository;
import shopstack_backend.repository.PaymentRepository;
import shopstack_backend.repository.ProductRepository;
import shopstack_backend.repository.RefundRepository;
import shopstack_backend.repository.UserRepository;
import java.math.BigDecimal;
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
    @Autowired
    private RefundRepository refundRepository;
    @Autowired
    private RefundService refundService;
    @Autowired(required = false)
    private ProductService productService;
    @Autowired
    private CommissionService commissionService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private OrderStatusService orderStatusService;
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
    @Transactional
    public OrderResponseDTO checkout(String email, Long addressId, String paymentMethod, String transactionId,
                                      String couponCode) {
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
            int available = warehouseService.getTotalAvailableStock(product.getId());
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
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            BigDecimal finalPrice = product.getFinalPrice();
BigDecimal lineTotal = finalPrice
        .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
        .setScale(2, java.math.RoundingMode.HALF_UP);
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPriceAtPurchase(finalPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(lineTotal);
            orderItem.setStatus(OrderStatus.CONFIRMED);
            orderItems.add(orderItem);
            total = total.add(lineTotal);
            // Stock is no longer decremented here — it now lives entirely at
            // the warehouse level (WarehouseStock), moved from available to
            // allocated by warehouseService.allocateOrderToWarehouses below,
            // right after the order is saved and confirmed.
        }
        order.setItems(orderItems);
        total = total.setScale(2, java.math.RoundingMode.HALF_UP);

        CouponService.CouponEvaluationResult couponEval = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            couponEval = couponService.validate(couponCode, total);
            couponService.reserveUsage(couponEval.getCoupon());
            discountAmount = couponEval.getDiscountAmount();
        }

        order.setSubtotalAmount(total);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponEval != null ? couponEval.getCoupon().getCode() : null);
        order.setTotalAmount(total.subtract(discountAmount).setScale(2, java.math.RoundingMode.HALF_UP));

        Order saved = orderRepository.save(order);
        Payment payment = recordPayment(saved, paymentMethod, transactionId);
        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);
        commissionService.syncCommissionsForOrder(saved);
        warehouseService.allocateOrderToWarehouses(saved);
        if (couponEval != null) {
            couponService.recordUsage(couponEval.getCoupon(), user, saved, discountAmount);
        }
        cart.getItems().clear();
        cartRepository.save(cart);
        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        OrderResponseDTO dto = mapToDTO(saved, payment);
        dto.setCustomerOrderNumber(userOrders.size());
        return dto;
    }
    @Transactional
    public OrderResponseDTO checkout(String email, Long addressId, String paymentMethod, String transactionId) {
        return checkout(email, addressId, paymentMethod, transactionId, null);
    }
    @Transactional
    public OrderResponseDTO checkout(String email) {
        Address defaultAddress = addressRepository.findByUserEmailAndIsDefaultTrue(email)
                .orElseThrow(() -> new IllegalStateException(
                        "No default address on file. Add a shipping address first."));
        return checkout(email, defaultAddress.getId(), "SIMULATED", "TXN-" + UUID.randomUUID());
    }
    @Transactional
    public OrderResponseDTO placeCodOrder(String email, Long addressId, String couponCode) {
        return checkoutWithStatus(email, addressId, "COD", "COD-" + UUID.randomUUID(), PaymentStatus.PENDING, couponCode);
    }
    private OrderResponseDTO checkoutWithStatus(String email, Long addressId, String paymentMethod,
                                                 String transactionId, PaymentStatus paymentStatus, String couponCode) {
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
            int available = warehouseService.getTotalAvailableStock(product.getId());
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
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
             BigDecimal finalPrice = product.getFinalPrice();
BigDecimal lineTotal = finalPrice
        .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
        .setScale(2, java.math.RoundingMode.HALF_UP);
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPriceAtPurchase(finalPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(lineTotal);
            orderItem.setStatus(OrderStatus.CONFIRMED);
            orderItems.add(orderItem);
      total = total.add(lineTotal);
            // Stock decrement now happens exclusively at the warehouse level
            // (see warehouseService.allocateOrderToWarehouses below).
        }
        order.setItems(orderItems);
        total = total.setScale(2, java.math.RoundingMode.HALF_UP);

        CouponService.CouponEvaluationResult couponEval = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            couponEval = couponService.validate(couponCode, total);
            couponService.reserveUsage(couponEval.getCoupon());
            discountAmount = couponEval.getDiscountAmount();
        }

        order.setSubtotalAmount(total);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponEval != null ? couponEval.getCoupon().getCode() : null);
        order.setTotalAmount(total.subtract(discountAmount).setScale(2, java.math.RoundingMode.HALF_UP));

        Order saved = orderRepository.save(order);
        Payment payment = recordPayment(saved, paymentMethod, transactionId, paymentStatus);
        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);
        commissionService.syncCommissionsForOrder(saved);
        warehouseService.allocateOrderToWarehouses(saved);
        if (couponEval != null) {
            couponService.recordUsage(couponEval.getCoupon(), user, saved, discountAmount);
        }
        cart.getItems().clear();
        cartRepository.save(cart);
        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        OrderResponseDTO dto = mapToDTO(saved, payment);
        dto.setCustomerOrderNumber(userOrders.size());
        return dto;
    }
    @Transactional
    public OrderResponseDTO checkoutSingleItem(String email, Long addressId, Long productId, Integer quantity,
                                                String paymentMethod, String transactionId, PaymentStatus paymentStatus,
                                                String couponCode) {
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
        int available = warehouseService.getTotalAvailableStock(product.getId());
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
        BigDecimal finalPrice = product.getFinalPrice();
BigDecimal lineTotal = finalPrice
        .multiply(BigDecimal.valueOf(quantity))
        .setScale(2, java.math.RoundingMode.HALF_UP);
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setProductName(product.getName());
        orderItem.setPriceAtPurchase(finalPrice);
        orderItem.setQuantity(quantity);
        orderItem.setLineTotal(lineTotal);
        orderItem.setStatus(OrderStatus.CONFIRMED);
        order.setItems(new ArrayList<>(List.of(orderItem)));

        CouponService.CouponEvaluationResult couponEval = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            couponEval = couponService.validate(couponCode, lineTotal);
            couponService.reserveUsage(couponEval.getCoupon());
            discountAmount = couponEval.getDiscountAmount();
        }

        order.setSubtotalAmount(lineTotal);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponEval != null ? couponEval.getCoupon().getCode() : null);
        order.setTotalAmount(lineTotal.subtract(discountAmount).setScale(2, java.math.RoundingMode.HALF_UP));

        Order saved = orderRepository.save(order);
        Payment payment = recordPayment(saved, paymentMethod, transactionId, paymentStatus);
        saved.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(saved);
        commissionService.syncCommissionsForOrder(saved);
        warehouseService.allocateOrderToWarehouses(saved);
        if (couponEval != null) {
            couponService.recordUsage(couponEval.getCoupon(), user, saved, discountAmount);
        }
        // Stock decrement now happens exclusively at the warehouse level
        // (see warehouseService.allocateOrderToWarehouses above).
        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        OrderResponseDTO dto = mapToDTO(saved, payment);
        dto.setCustomerOrderNumber(userOrders.size());
        return dto; }
    @Transactional
    public OrderResponseDTO placeCodBuyNowOrder(String email, Long addressId, Long productId, Integer quantity,
                                                 String couponCode) {
        return checkoutSingleItem(email, addressId, productId, quantity,
                "COD", "COD-" + UUID.randomUUID(), PaymentStatus.PENDING, couponCode);
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
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrderHistory(String email) {
        List<Order> orders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        List<OrderResponseDTO> dtoList = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            OrderResponseDTO dto = mapToDTO(order, payment);
            dto.setCustomerOrderNumber(i + 1); // 1-indexed count per user
            dtoList.add(dto);  }
        Collections.reverse(dtoList);
        return dtoList;
    }
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId, String email) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, email)
                .orElseThrow(() -> new SecurityException("Order not found for this account."));
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);  
        OrderResponseDTO dto = mapToDTO(order, payment);
        List<Order> userOrders = orderRepository.findByUserEmailOrderByCreatedAtAsc(email);
        for (int i = 0; i < userOrders.size(); i++) {
            if (userOrders.get(i).getId().equals(order.getId())) {
                dto.setCustomerOrderNumber(i + 1);
                break;
            }
        }    return dto;
    }
    @Transactional(readOnly = true)
    public List<VendorOrderItemResponseDTO> getVendorOrderItems(String vendorEmail) {
        return orderItemRepository
                .findByProduct_Vendor_User_EmailOrderByOrder_CreatedAtDesc(vendorEmail)
                .stream()
                .map(this::mapToVendorDTO)
                .collect(Collectors.toList());
    }
   @Transactional
public OrderResponseDTO markItemDelivered(Long orderItemId) {

    OrderItem item = orderItemRepository.findById(orderItemId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Order item not found."));

    // Prevent Mark Delivered from being clicked again.
    if (item.getStatus() == OrderStatus.DELIVERED) {
        throw new IllegalStateException(
                "This item has already been delivered."
        );
    }
    if (item.getStatus() != OrderStatus.SHIPPED) {
        throw new IllegalStateException(
                "This item is currently " + item.getStatus()
                        + " and has not been shipped yet."
        );
    }

    // Update the actual order-item status.
    item.setStatus(OrderStatus.DELIVERED);
    orderItemRepository.save(item);
    warehouseService.markAllocationsDelivered(item);

    // Recalculate the parent order status.
    Order order = item.getOrder();
    orderStatusService.recomputeOrderStatus(order);
    Payment payment = paymentRepository
            .findByOrderId(order.getId())
            .orElse(null);

    return mapToDTO(order, payment);
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
        if (newStatus == OrderStatus.CANCELLED) {
            if (currentStatus == OrderStatus.SHIPPED || currentStatus == OrderStatus.DELIVERED) {
                throw new IllegalStateException(
                        "This item has already " +
                        (currentStatus == OrderStatus.DELIVERED ? "been delivered" : "shipped") +
                        " and can no longer be cancelled here. Use the returns process instead.");
            }
        } else {
            if (warehouseService.isWarehouseManaged(item.getId())) {
                throw new IllegalStateException(
                        "This item is being handled by the warehouse fulfillment pipeline " +
                        "(pick \u2192 pack \u2192 ship) and updates automatically as it moves through " +
                        "that process \u2014 no manual status change is needed here.");
            }
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
       orderStatusService.recomputeOrderStatus(item.getOrder());
        commissionService.syncCommissionsForOrder(item.getOrder());
        if (newStatus == OrderStatus.CANCELLED) {
            warehouseService.releaseAllocationsForItem(item);
        }
        return mapToVendorDTO(item);
    }
    @Transactional
    public OrderResponseDTO cancelOrderItem(String customerEmail, Long orderItemId) {
        OrderItem item = orderItemRepository.findByIdAndOrder_User_Email(orderItemId, customerEmail)
                .orElseThrow(() -> new SecurityException(
                        "This order item doesn't exist or doesn't belong to your account."));
        OrderStatus currentStatus = item.getStatus();
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException("This item is already " + currentStatus + ".");  }
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "This item is already " + currentStatus + " and can no longer be cancelled. " +
                    "Contact the seller if you need to return it once delivered.");  }
        Product product = item.getProduct();
        // Stock restoration now happens entirely at the warehouse level via
        // warehouseService.releaseAllocationsForItem below — Product.stockQuantity
        // represents the vendor's undistributed pool and was already "spent"
        // at distribution time, not at sale time, so it isn't touched here.
        item.setStatus(OrderStatus.CANCELLED);
        orderItemRepository.save(item);
        Order order = item.getOrder();
        orderStatusService.recomputeOrderStatus(order);
        commissionService.syncCommissionsForOrder(order);
        warehouseService.releaseAllocationsForItem(item);
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        refundIfPaidOnline(item, payment);
        return mapToDTO(order, payment);
    }
    @Transactional
    public OrderResponseDTO cancelOrder(String customerEmail, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, customerEmail)
                .orElseThrow(() -> new SecurityException("Order not found for this account."));
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        boolean cancelledAny = false;
        for (OrderItem item : order.getItems()) {
            OrderStatus status = item.getStatus();
            if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
                // Stock restoration happens at the warehouse level via
                // warehouseService.releaseAllocationsForItem below.
                item.setStatus(OrderStatus.CANCELLED);
                orderItemRepository.save(item);
                cancelledAny = true;
                refundIfPaidOnline(item, payment);
                warehouseService.releaseAllocationsForItem(item);
            }
        }
      if (!cancelledAny) {
            throw new IllegalStateException(
                    "None of the items in this order can be cancelled anymore — they're already being processed.");
        }
        orderStatusService.recomputeOrderStatus(order);
        commissionService.syncCommissionsForOrder(order);
        return mapToDTO(order, payment);
    }
    private void refundIfPaidOnline(OrderItem item, Payment payment) {
        if (payment == null) return;
        if (!"RAZORPAY".equalsIgnoreCase(payment.getMethod())) return;
        if (payment.getStatus() != PaymentStatus.SUCCESS) return;
        if (refundRepository.findByOrderItemId(item.getId()).isPresent()) return; // already refunded once
        refundService.processRefund(item);
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
        dto.setWarehouseManaged(warehouseService.isWarehouseManaged(item.getId()));
        dto.setShippingFullName(order.getShippingFullName());
        dto.setShippingPhone(order.getShippingPhone());
        dto.setShippingAddressLine1(order.getShippingAddressLine1());
        dto.setShippingAddressLine2(order.getShippingAddressLine2());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingPostalCode(order.getShippingPostalCode());
        dto.setShippingCountry(order.getShippingCountry());
        return dto;}
    private OrderResponseDTO mapToDTO(Order order, Payment payment) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSubtotalAmount(order.getSubtotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setCouponCode(order.getCouponCode());
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
            refundRepository.findByOrderItemId(item.getId()).ifPresent(refund -> {
                RefundResponseDTO refundDto = new RefundResponseDTO();
                refundDto.setAmount(refund.getAmount());
                refundDto.setMethod(refund.getMethod());
                refundDto.setStatus(refund.getStatus().name());
                refundDto.setGatewayRefundId(refund.getGatewayRefundId());
                refundDto.setProcessedAt(refund.getProcessedAt());
                refundDto.setFailureReason(refund.getFailureReason());
                itemDto.setRefund(refundDto);
            });
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