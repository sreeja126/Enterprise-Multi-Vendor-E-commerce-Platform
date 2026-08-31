package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shopstack_backend.entity.Order;
import shopstack_backend.entity.OrderItem;
import shopstack_backend.entity.OrderStatus;
import shopstack_backend.repository.OrderRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Recomputes an Order's overall status from its items' individual statuses.
 * Pulled out of OrderService into its own bean (depending on nothing but
 * OrderRepository) so that WarehouseService — which OrderService already
 * depends on — can also trigger a recompute after auto-advancing an item's
 * status (e.g. PROCESSING on allocation, SHIPPED on ready-for-shipment)
 * without creating a circular OrderService <-> WarehouseService dependency.
 */
@Service
public class OrderStatusService {

    private static final List<OrderStatus> PROGRESSION = Arrays.asList(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public void recomputeOrderStatus(Order order) {
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
        if (least == null) {
            boolean allCancelled = items.stream().allMatch(i -> i.getStatus() == OrderStatus.CANCELLED);
            order.setStatus(allCancelled ? OrderStatus.CANCELLED : order.getStatus());
        } else {
            order.setStatus(least);
        }
        orderRepository.save(order);
    }
}