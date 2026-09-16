package com.zzowo.shop_sys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 啟用 @Scheduled 排程 (目前用於逾時未付款訂單的自動取消)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
