package com.zzowo.shop_sys.scheduler;

import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.repository.OrderRepository;
import com.zzowo.shop_sys.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExpirationSchedulerTest {

    private static final int TIMEOUT_MINUTES = 30;

    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;
    @InjectMocks OrderExpirationScheduler scheduler;

    @BeforeEach
    void setTimeout() {
        // @Value 不會在單元測試注入,手動設定
        ReflectionTestUtils.setField(scheduler, "timeoutMinutes", TIMEOUT_MINUTES);
    }

    @Test
    void cancelExpiredOrders_queriesPendingOrdersOlderThanTimeout() {
        when(orderRepository.findIdsByStatusAndCreatedAtBefore(any(), any(), any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        scheduler.cancelExpiredOrders();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findIdsByStatusAndCreatedAtBefore(
                eq(OrderStatus.PENDING), cutoffCaptor.capture(), any(Pageable.class));

        // cutoff 應落在「呼叫前算出的門檻」與「現在減去門檻」之間
        assertThat(cutoffCaptor.getValue())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES));
    }

    @Test
    void cancelExpiredOrders_noExpiredOrders_cancelsNothing() {
        when(orderRepository.findIdsByStatusAndCreatedAtBefore(any(), any(), any())).thenReturn(List.of());

        scheduler.cancelExpiredOrders();

        verify(orderService, never()).cancelExpiredOrder(anyLong());
    }

    @Test
    void cancelExpiredOrders_cancelsEveryExpiredOrder() {
        when(orderRepository.findIdsByStatusAndCreatedAtBefore(any(), any(), any()))
                .thenReturn(List.of(1L, 2L, 3L));
        when(orderService.cancelExpiredOrder(anyLong())).thenReturn(true);

        scheduler.cancelExpiredOrders();

        verify(orderService).cancelExpiredOrder(1L);
        verify(orderService).cancelExpiredOrder(2L);
        verify(orderService).cancelExpiredOrder(3L);
    }

    @Test
    void cancelExpiredOrders_oneOrderThrows_continuesWithRest() {
        when(orderRepository.findIdsByStatusAndCreatedAtBefore(any(), any(), any()))
                .thenReturn(List.of(1L, 2L, 3L));
        when(orderService.cancelExpiredOrder(anyLong())).thenReturn(true);
        // 只有第 2 筆失敗 (用 doThrow 覆寫,避免未 stub 的引數觸發 strict stub 例外)
        doThrow(new RuntimeException("DB 連線中斷")).when(orderService).cancelExpiredOrder(2L);

        scheduler.cancelExpiredOrders();

        // 單筆失敗不中斷整批,後面的訂單照樣處理
        verify(orderService).cancelExpiredOrder(1L);
        verify(orderService).cancelExpiredOrder(3L);
    }
}
