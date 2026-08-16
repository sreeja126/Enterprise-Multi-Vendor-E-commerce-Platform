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

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // Step 1: create a Razorpay order for whatever the customer's cart
    // currently totals to (using finalPrice — the discounted amount — same
    // as everywhere else). The frontend uses this to open the Razorpay
    // Checkout modal.
    public RazorpayOrderResponseDTO createRazorpayOrder(String email) throws Exception {
        CartResponseDTO cart = cartService.getCart(email);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        // Razorpay expects the amount in paise (smallest currency unit),
        // as an integer — never send a fractional rupee amount.
        long amountInPaise = Math.round(cart.getTotalAmount() * 100);

        JSONObject options = new JSONObject();
        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put("receipt", "cart_" + email + "_" + System.currentTimeMillis());

        Order razorpayOrder = razorpayClient.orders.create(options);

        return new RazorpayOrderResponseDTO(
                razorpayOrder.get("id"),
                keyId,
                amountInPaise,
                "INR"
        );
    }

    // Buy Now variant — amount is derived from ONE product × quantity,
    // never from the cart. This is what makes Buy Now genuinely independent
    // of whatever else the customer already has sitting in their cart.
    public RazorpayOrderResponseDTO createRazorpayOrderForProduct(String email, Long productId, Integer quantity)
            throws Exception {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (available < quantity) {
            throw new IllegalStateException(
                    "\"" + product.getName() + "\" only has " + available + " unit(s) left.");
        }

        long amountInPaise = Math.round(product.getFinalPrice() * quantity * 100);

        JSONObject options = new JSONObject();
        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put("receipt", "buynow_" + email + "_" + System.currentTimeMillis());

        Order razorpayOrder = razorpayClient.orders.create(options);

        return new RazorpayOrderResponseDTO(
                razorpayOrder.get("id"),
                keyId,
                amountInPaise,
                "INR"
        );
    }

    // Step 2: after the customer completes payment in the Razorpay modal,
    // the frontend sends back razorpay_order_id, razorpay_payment_id, and
    // razorpay_signature. We MUST verify the signature ourselves — never
    // trust the frontend's word that payment succeeded, since a malicious
    // client could just call this endpoint directly with fake IDs and no
    // signature at all.
    public OrderResponseDTO verifyAndCompleteOrder(String email, VerifyPaymentRequest request) throws Exception {

        if (request.getRazorpayOrderId() == null ||
            request.getRazorpayPaymentId() == null ||
            request.getRazorpaySignature() == null) {
            throw new SecurityException("Incomplete payment verification data.");
        }

        if (request.getAddressId() == null) {
            throw new IllegalArgumentException("Please select a shipping address before checking out.");
        }

        verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        // Signature is valid — genuinely paid via Razorpay. Now create the
        // actual order (validates stock again, snapshots prices and the
        // chosen address, reduces stock, clears cart) and record this
        // real transaction ID.
        return orderService.checkout(email, request.getAddressId(), "RAZORPAY", request.getRazorpayPaymentId());
    }

    // Buy Now variant — same signature verification, but completes a
    // single-item order instead of the whole cart.
    public OrderResponseDTO verifyAndCompleteBuyNowOrder(String email, VerifyBuyNowRequest request) throws Exception {

        if (request.getRazorpayOrderId() == null ||
            request.getRazorpayPaymentId() == null ||
            request.getRazorpaySignature() == null) {
            throw new SecurityException("Incomplete payment verification data.");
        }

        if (request.getAddressId() == null) {
            throw new IllegalArgumentException("Please select a shipping address before checking out.");
        }

        verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        return orderService.checkoutSingleItem(
                email, request.getAddressId(), request.getProductId(), request.getQuantity(),
                "RAZORPAY", request.getRazorpayPaymentId(),
                shopstack_backend.entity.PaymentStatus.SUCCESS
        );
    }

    private void verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature)
            throws Exception {
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);

        boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);

        if (!isValid) {
            throw new SecurityException("Payment verification failed. This payment could not be confirmed.");
        }
    }
}