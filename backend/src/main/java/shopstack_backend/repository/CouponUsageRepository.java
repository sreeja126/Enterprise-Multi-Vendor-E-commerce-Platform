package shopstack_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shopstack_backend.entity.CouponUsage;

import java.math.BigDecimal;
import java.util.List;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    List<CouponUsage> findAllByOrderByUsedAtDesc();

    List<CouponUsage> findByCoupon_IdOrderByUsedAtDesc(Long couponId);

    @Query("SELECT COALESCE(SUM(cu.discountAmount), 0) FROM CouponUsage cu WHERE cu.coupon.id = :couponId")
    BigDecimal sumDiscountByCouponId(@Param("couponId") Long couponId);
}