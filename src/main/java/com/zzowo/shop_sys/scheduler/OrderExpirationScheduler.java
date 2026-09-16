package com.zzowo.shop_sys.scheduler;

import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.repository.OrderRepository;
import com.zzowo.shop_sys.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 定期取消逾時未付款的訂單,把被佔用的庫存還回去
// 與 OrderService 分開是必要的: 同類自我呼叫會繞過交易代理,導致每筆訂單沒有各自的交易
@Slf4j
@Component
@ConditionalOnProperty(name = "app.order.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationScheduler {

    // 單次排程最多處理的訂單數,避免累積過多時一次撈爆記憶體
    private static final int BATCH_SIZE = 200;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Value("${app.order.expiration.timeout-minutes}")
    private int timeoutMinutes;

    @Scheduled(fixedDelayString = "${app.order.expiration.check-interval-ms}")
    public void cancelExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<Long> expiredIds = orderRepository.findIdsByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, cutoff, PageRequest.of(0, BATCH_SIZE));

        if (expiredIds.isEmpty()) {
            return;
        }

        int cancelled = 0;
        for (Long orderId : expiredIds) {
            try {
                if (orderService.cancelExpiredOrder(orderId)) {
                    cancelled++;
                }
            } catch (Exception e) {
                // 單筆失敗不該中斷整批,下一輪排程還會再試
                log.error("取消逾時訂單失敗,orderId={}", orderId, e);
            }
        }

        log.info("逾時未付款訂單處理完成,掃描 {} 筆,取消 {} 筆 (逾時門檻 {} 分鐘) ",
                expiredIds.size(), cancelled, timeoutMinutes);
    }
}
