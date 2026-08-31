package shopstack_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shopstack_backend.entity.StockMovement;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByOrderItem_IdOrderByMovedAtAsc(Long orderItemId);

    List<StockMovement> findByWarehouse_IdOrderByMovedAtDesc(Long warehouseId);

    List<StockMovement> findAllByOrderByMovedAtDesc();
}