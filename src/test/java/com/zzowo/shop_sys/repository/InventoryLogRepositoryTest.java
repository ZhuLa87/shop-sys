package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.InventoryLog;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InventoryLogRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired InventoryLogRepository inventoryLogRepository;

    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        productA = em.persistAndFlush(buildProduct("商品A"));
        productB = em.persistAndFlush(buildProduct("商品B"));
    }

    @Test
    void findByProductIdOrderByCreatedAtDesc_returnsOnlyTargetProductLogsNewestFirst() {
        InventoryLog log1 = em.persistAndFlush(buildLog(productA, -2, "ORDER"));
        InventoryLog log2 = em.persistAndFlush(buildLog(productA, 10, "RESTOCK"));
        em.persistAndFlush(buildLog(productB, -1, "ORDER")); // 不同商品,不應出現

        setCreatedAt(log1.getId(), LocalDateTime.now().minusHours(1));
        setCreatedAt(log2.getId(), LocalDateTime.now());
        em.flush();
        em.clear();

        List<InventoryLog> result = inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productA.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(log2.getId()); // 最新在前
        assertThat(result.get(0).getProduct().getName()).isEqualTo("商品A"); // JOIN FETCH 確認
        assertThat(result.get(1).getId()).isEqualTo(log1.getId());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_returnsAllLogsNewestFirst() {
        InventoryLog log1 = em.persistAndFlush(buildLog(productA, -2, "ORDER"));
        InventoryLog log2 = em.persistAndFlush(buildLog(productB, 5, "RESTOCK"));
        InventoryLog log3 = em.persistAndFlush(buildLog(productA, 10, "ADJUSTMENT"));

        setCreatedAt(log1.getId(), LocalDateTime.now().minusHours(2));
        setCreatedAt(log2.getId(), LocalDateTime.now().minusHours(1));
        setCreatedAt(log3.getId(), LocalDateTime.now());
        em.flush();
        em.clear();

        List<InventoryLog> result = inventoryLogRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(log3.getId()); // 最新在前
        assertThat(result.get(2).getId()).isEqualTo(log1.getId()); // 最舊在後
        // JOIN FETCH 確認 product 已載入
        assertThat(result.get(0).getProduct()).isNotNull();
    }

    @Test
    void operatorSnapshot_isPersistedAndReadBack() {
        // 名稱與角色是寫入當下的快照,之後使用者改名或軟刪除都不影響這筆紀錄
        InventoryLog log = buildLog(productA, 10, "RESTOCK");
        log.applyOperator(buildOperator());
        Long id = em.persistAndFlush(log).getId();
        em.clear();

        InventoryLog found = inventoryLogRepository.findById(id).orElseThrow();

        assertThat(found.getOperatorId()).isEqualTo(7L);
        assertThat(found.getOperatorName()).isEqualTo("庫存管理員");
        assertThat(found.getOperatorRole()).isEqualTo(Role.PRODUCT_MANAGER);
    }

    @Test
    void operatorSnapshot_systemOperation_leavesAllOperatorFieldsNull() {
        InventoryLog log = buildLog(productA, 3, "CANCEL");
        log.applyOperator(null);
        Long id = em.persistAndFlush(log).getId();
        em.clear();

        InventoryLog found = inventoryLogRepository.findById(id).orElseThrow();

        assertThat(found.getOperatorId()).isNull();
        assertThat(found.getOperatorName()).isNull();
        assertThat(found.getOperatorRole()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setCreatedAt(Long logId, LocalDateTime time) {
        em.getEntityManager()
                .createNativeQuery("UPDATE inventory_logs SET created_at = ? WHERE id = ?")
                .setParameter(1, Timestamp.valueOf(time))
                .setParameter(2, logId)
                .executeUpdate();
    }

    private Product buildProduct(String name) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(200));
        p.setStockQuantity(50);
        p.setStatus(ProductStatus.ON_SHELF);
        return p;
    }

    private InventoryLog buildLog(Product product, int changeAmount, String reason) {
        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setChangeAmount(changeAmount);
        log.setReason(reason);
        return log;
    }

    private User buildOperator() {
        User user = new User();
        user.setId(7L);
        user.setEmail("manager@test.com");
        user.setName("庫存管理員");
        user.setRole(Role.PRODUCT_MANAGER);
        return user;
    }
}
