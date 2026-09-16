package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.Order;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired OrderRepository orderRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(buildUser("buyer@test.com"));
    }

    @Test
    void findByUserId_returnsOnlyTargetUserOrders() {
        User other = em.persistAndFlush(buildUser("other@test.com"));
        em.persistAndFlush(buildOrder(user));
        em.persistAndFlush(buildOrder(other));

        List<Order> result = orderRepository.findByUserId(user.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsNewestFirst() {
        Order o1 = em.persistAndFlush(buildOrder(user));
        Order o2 = em.persistAndFlush(buildOrder(user));
        Order o3 = em.persistAndFlush(buildOrder(user));

        // 用 native query 設定明確的時間差,避免依賴 @PrePersist 精度
        setCreatedAt(o1.getId(), LocalDateTime.now().minusHours(2));
        setCreatedAt(o2.getId(), LocalDateTime.now().minusHours(1));
        setCreatedAt(o3.getId(), LocalDateTime.now());
        em.flush();
        em.clear();

        List<Order> result = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(o3.getId()); // 最新在前
        assertThat(result.get(2).getId()).isEqualTo(o1.getId()); // 最舊在後
    }

    // ── 逾時未付款訂單的自動取消 ──────────────────────────────────────────────

    @Test
    void findIdsByStatusAndCreatedAtBefore_returnsOnlyExpiredPendingOrders() {
        Order expired = em.persistAndFlush(buildOrder(user));
        Order fresh = em.persistAndFlush(buildOrder(user));
        Order expiredButPaid = em.persistAndFlush(buildOrder(user));
        expiredButPaid.setStatus(OrderStatus.PAID);

        setCreatedAt(expired.getId(), LocalDateTime.now().minusHours(2));
        setCreatedAt(expiredButPaid.getId(), LocalDateTime.now().minusHours(2));
        em.flush();
        em.clear();

        List<Long> result = orderRepository.findIdsByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, LocalDateTime.now().minusMinutes(30), PageRequest.of(0, 200));

        assertThat(result).containsExactly(expired.getId());
        assertThat(result).doesNotContain(fresh.getId(), expiredButPaid.getId());
    }

    @Test
    void cancelIfPending_pendingOrder_returns1_andStatusBecomesCancelled() {
        Order order = em.persistAndFlush(buildOrder(user));
        em.clear();

        int updated = orderRepository.cancelIfPending(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED);

        em.clear();
        assertThat(updated).isEqualTo(1);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelIfPending_paidOrder_returns0_andStatusUnchanged() {
        // 已付款的訂單不可被排程取消,庫存也就不會被誤補
        Order order = buildOrder(user);
        order.setStatus(OrderStatus.PAID);
        em.persistAndFlush(order);
        em.clear();

        int updated = orderRepository.cancelIfPending(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED);

        em.clear();
        assertThat(updated).isZero();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setCreatedAt(Long orderId, LocalDateTime time) {
        em.getEntityManager()
                .createNativeQuery("UPDATE orders SET created_at = ? WHERE id = ?")
                .setParameter(1, Timestamp.valueOf(time))
                .setParameter(2, orderId)
                .executeUpdate();
    }

    private User buildUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setRole(Role.CUSTOMER);
        return u;
    }

    private Order buildOrder(User u) {
        Order o = new Order();
        o.setUser(u);
        o.setTotalAmount(BigDecimal.valueOf(999));
        o.setStatus(OrderStatus.PENDING);
        o.setRecipientName("測試收件人");
        o.setRecipientPhone("0912345678");
        o.setRecipientAddress("台北市信義區");
        return o;
    }
}
