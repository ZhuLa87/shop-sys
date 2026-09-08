#!/bin/sh
# MinIO 一次性初始化.由 compose 的 minio-init 服務執行,結束後容器即退出.
# 設計為可重複執行 (idempotent) :重跑 compose up 不會出錯.
#
# backend 以 depends_on: condition: service_completed_successfully 等這個容器,
# 因此這裡任何一步失敗都必須以非零狀態結束 —— 卡住不退出會讓整個 stack 靜默停擺.
set -eu

# compose 已用 depends_on: service_healthy 等過 MinIO,這裡的重試只是雙重保險.
# 但不能無限等:密碼錯誤之類的設定問題會讓連線永遠不成功,
# 屆時 minio-init 會一直卡著,backend 永遠等不到,而且不會有任何錯誤訊息.
MAX_RETRIES=30
echo "[minio-init] 等待 MinIO 就緒 (最多 ${MAX_RETRIES} 次) ..."
until mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
    MAX_RETRIES=$((MAX_RETRIES - 1))
    if [ "$MAX_RETRIES" -le 0 ]; then
        echo "[minio-init] 連線失敗,已達重試上限." >&2
        echo "[minio-init] 常見原因: MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 與 minio 服務的設定不一致" >&2
        echo "[minio-init] (改過密碼但沿用舊的 minio-data volume 也會這樣,舊憑證仍留在 volume 裡) " >&2
        # 最後再跑一次且不吞掉輸出,讓真正的錯誤訊息進到 log
        mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" || true
        exit 1
    fi
    sleep 1
done

echo "[minio-init] 建立 bucket: $MINIO_BUCKET"
mc mb --ignore-existing "local/$MINIO_BUCKET"

# 商品圖片與頭像由瀏覽器直接以 public-url 讀取 (不帶簽章) ,因此需要匿名下載權限.
# 只開放 download,不開放 list/upload.
echo "[minio-init] 設定匿名讀取權限"
mc anonymous set download "local/$MINIO_BUCKET"

# 後端使用專屬帳號而非 root 金鑰
if mc admin user info local "$MINIO_ACCESS_KEY" >/dev/null 2>&1; then
    echo "[minio-init] 後端存取金鑰已存在,略過建立"
else
    echo "[minio-init] 建立後端存取金鑰: $MINIO_ACCESS_KEY"
    mc admin user add local "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
fi

# 已套用時 attach 會回非零,所以不能直接讓 set -e 中斷;
# 但也不能無條件忽略 —— 真的沒掛上政策的話,後端會拿到一個沒有任何權限的帳號,
# 要等到第一次上傳圖片才會發現.因此改為套用後回頭驗證.
mc admin policy attach local readwrite --user "$MINIO_ACCESS_KEY" >/dev/null 2>&1 || true
# 用 shell 內建的 case 比對而非 grep:minio/mc 是極簡映像,連 grep 都沒有
USER_INFO=$(mc admin user info local "$MINIO_ACCESS_KEY" 2>/dev/null || true)
case "$USER_INFO" in
    *readwrite*)
        echo "[minio-init] readwrite 政策已套用"
        ;;
    *)
        echo "[minio-init] 無法為 $MINIO_ACCESS_KEY 套用 readwrite 政策" >&2
        echo "$USER_INFO" >&2
        exit 1
        ;;
esac

echo "[minio-init] 完成"
