package shopstack_backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import shopstack_backend.dto.CartResponseDTO;
import shopstack_backend.dto.OrderResponseDTO;
import shopstack_backend.dto.RazorpayOrderResponseDTO;
import shopstack_backend.dto.VerifyBuyNowRequest;
import shopstack_backend.dto.VerifyPaymentRequest;
import shopstack_backend.entity.Product;
import shopstack_backend.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayPaymentService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponService couponService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public RazorpayOrderResponseDTO createRazorpayOrder(String email, String couponCode) throws Exception {

        CartResponseDTO cart = cartService.getCart(email);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        BigDecimal totalAmount = cart.getTotalAmount();

        // If a coupon code was supplied, charge the discounted amount instead
        // of the full subtotal. Re-validated (and actually reserved) again
        // at /payment/verify time, so this is a preview, not a guarantee.
        if (couponCode != null && !couponCode.isBlank()) {
            CouponService.CouponEvaluationResult eval = couponService.validate(couponCode, totalAmount);
            totalAmount = totalAmount.subtract(eval.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
        }

        long amountInPaise = totalAmount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        JSONObject options = new JSONObject();

        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put(
                "receipt",
                "cart_" + email + "_" + System.currentTimeMillis()
        );

        Order razorpayOrder = razorpayClient.orders.create(options);

        return new RazorpayOrderResponseDTO(
                razorpayOrder.get("id"),
                keyId,
                amountInPaise,
                "INR"
        );
    }

    public RazorpayOrderResponseDTO createRazorpayOrderForProduct(
            String email,
            Long productId,
            Integer quantity,
            String couponCode
    ) throws Exception {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        int available = product.getStockQuantity() != null
                ? product.getStockQuantity()
                : 0;

        if (available < quantity) {
            throw new IllegalStateException(
                    "\"" + product.getName() + "\" only has "
                            + available + " unit(s) left."
            );
        }

        BigDecimal amount = product
                .getFinalPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        if (couponCode != null && !couponCode.isBlank()) {
            CouponService.CouponEvaluationResult eval = couponService.validate(couponCode, amount);
            amount = amount.subtract(eval.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
        }

        long amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        JSONObject options = new JSONObject();

        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put(
                "receipt",
                "buynow_" + email + "_" + System.currentTimeMillis()
        );

        Order razorpayOrder = razorpayClient.orders.create(options);

        return new RazorpayOrderResponseDTO(
                razorpayOrder.get("id"),
                keyId,
                amountInPaise,
                "INR"
        );
    }

    public OrderResponseDTO verifyAndCompleteOrder(
            String email,
            VerifyPaymentRequest request
    ) throws Exception {

        if (request.getRazorpayOrderId() == null
                || request.getRazorpayPaymentId() == null
                || request.getRazorpaySignature() == null) {

            throw new SecurityException(
                    "Incomplete payment verification data."
            );
        }

        if (request.getAddressId() == null) {
            throw new IllegalArgumentException(
                    "Please select a shipping address before checking out."
            );
        }

        verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        return orderService.checkout(
                email,
                request.getAddressId(),
                "RAZORPAY",
                request.getRazorpayPaymentId(),
                request.getCouponCode()
        );
    }

    public OrderResponseDTO verifyAndCompleteBuyNowOrder(
            String email,
            VerifyBuyNowRequest request
    ) throws Exception {

        if (request.getRazorpayOrderId() == null
                || request.getRazorpayPaymentId() == null
                || request.getRazorpaySignature() == null) {

            throw new SecurityException(
                    "Incomplete payment verification data."
            );
        }

        if (request.getAddressId() == null) {
            throw new IllegalArgumentException(
                    "Please select a shipping address before checking out."
            );
        }

        verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        return orderService.checkoutSingleItem(
                email,
                request.getAddressId(),
                request.getProductId(),
                request.getQuantity(),
                "RAZORPAY",
                request.getRazorpayPaymentId(),
                shopstack_backend.entity.PaymentStatus.SUCCESS,
                request.getCouponCode()
        );
    }

    private void verifySignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) throws Exception {

        JSONObject attributes = new JSONObject();

        attributes.put(
                "razorpay_order_id",
                razorpayOrderId
        );

        attributes.put(
                "razorpay_payment_id",
                razorpayPaymentId
        );

        attributes.put(
                "razorpay_signature",
                razorpaySignature
        );

        boolean isValid = Utils.verifyPaymentSignature(
                attributes,
                keySecret
        );

        if (!isValid) {
            throw new SecurityException(
                    "Payment verification failed. "
                            + "This payment could not be confirmed."
            );
        }
    }
}