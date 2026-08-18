package shopstack_backend.service;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import shopstack_backend.entity.*;
import shopstack_backend.repository.PaymentRepository;
import shopstack_backend.repository.RefundRepository;

import java.time.LocalDateTime;

@Service
public class RefundService {

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    // Processes a refund for exactly one order item's amount. For a
    // RAZORPAY-paid order, this calls Razorpay's real refund API (test
    // mode — nothing real moves, but the call and response are genuine).
    // For COD/SIMULATED orders, there's no online transaction to reverse,
    // so this just records a manual refund instead.
    public Refund processRefund(OrderItem item) {

        Payment payment = paymentRepository.findByOrderId(item.getOrder().getId()).orElse(null);

        Refund refund = new Refund();
        refund.setOrderItem(item);
        refund.setAmount(item.getLineTotal());

        if (payment != null && "RAZORPAY".equalsIgnoreCase(payment.getMethod())) {
            refund.setMethod("RAZORPAY");
            try {
                JSONObject options = new JSONObject();
                // Partial refund — just this item's amount, in paise.
                options.put("amount", Math.round(item.getLineTotal() * 100));

                com.razorpay.Refund razorpayRefund =
                        razorpayClient.payments.refund(payment.getTransactionId(), options);

                refund.setGatewayRefundId(razorpayRefund.get("id"));
                refund.setStatus(RefundStatus.PROCESSED);
                refund.setProcessedAt(LocalDateTime.now());

            } catch (Exception e) {
                // Don't let a failed gateway call silently disappear — the
                // return itself still gets approved (the physical item is
                // being returned regardless), but the refund needs manual
                // follow-up if this happens.
                refund.setStatus(RefundStatus.FAILED);
                refund.setFailureReason(e.getMessage());
            }

        } else {
            // COD / SIMULATED — nothing to reverse online. Mark as
            // processed immediately; in a real system this would be a
            // manual cash refund a vendor/admin confirms separately, but
            // that workflow is out of scope for now.
            refund.setMethod(payment != null ? payment.getMethod() : "MANUAL");
            refund.setStatus(RefundStatus.PROCESSED);
            refund.setProcessedAt(LocalDateTime.now());
        }

        return refundRepository.save(refund);
    }
}