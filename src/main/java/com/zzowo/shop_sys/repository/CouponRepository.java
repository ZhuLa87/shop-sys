package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // 根據折扣碼找優惠券
    Optional<Coupon> findByCode(String code);
}