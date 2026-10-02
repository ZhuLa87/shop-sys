package com.zzowo.shop_sys.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

// 在真正的 MariaDB 上跑的整合測試共用設定.
// H2 的鎖行為和 InnoDB 不同, 併發與 native UPDATE 相關的保證只能在真的資料庫上證明.
// schema 由 Flyway migration 建立 (同時驗證 migration 與 entity 一致), 沒有 Docker 時整個類別略過.
// 子類別共用同一個 container 與 Spring context; 測試不加 @Transactional, 以新建的資料互相隔離.
// container 用 singleton 寫法 (static 區塊啟動, 不加 @Container): @Container 會在每個測試類別結束時停掉 container,
// 但被快取的 Spring context 仍指向舊的 port, 下一個類別就連不上. JVM 結束時由 Testcontainers (Ryuk) 清除
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        // 連線池要夠大, 否則併發請求會在 Hikari 排隊, 實際上沒有同時打進 DB
        "spring.datasource.hikari.maximum-pool-size=50"
})
abstract class AbstractMariaDbIntegrationTest {

    // 與 compose.yaml 使用相同版本
    @ServiceConnection
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    static {
        mariadb.start();
    }
}
