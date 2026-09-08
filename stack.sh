#!/usr/bin/env bash
# shop-sys docker 操作入口 (dev / staging / prod 共用)
#
# compose 指令要同時帶 --env-file 與兩個 -f,很容易打錯,包成這個腳本.
# 純 bash + docker,不需要額外安裝任何工具.
#
#   ./stack.sh up                啟動開發環境
#   ./stack.sh logs              追蹤 log
#   ./stack.sh staging up        對 staging 環境執行
#   ./stack.sh --help
set -euo pipefail

cd "$(dirname "$0")"

ENV_NAME=dev

# 第一個參數若是環境名稱就取用,否則預設 dev
case "${1:-}" in
    dev|staging|prod) ENV_NAME=$1; shift ;;
esac

ENV_FILE="env/.env.${ENV_NAME}"
COMPOSE=(docker compose --env-file "$ENV_FILE" -f compose.yaml -f "compose.${ENV_NAME}.yaml")

usage() {
    cat <<'EOF'
用法: ./stack.sh [環境] <指令>

環境 (預設 dev):
  dev | staging | prod

指令:
  up          啟動 (dev 掛載原始碼;staging/prod 會重新 build)
  down        停止並移除容器,保留資料 volume
  reset       停止並刪除資料 volume,下次啟動會重建空資料庫
  restart     重啟容器,未指定服務時只重啟 backend (例: restart nginx)
  build       重新 build image
  logs        追蹤全部服務的 log (Ctrl-C 離開)
  ps          服務狀態與健康檢查結果
  config      展開後的完整 compose 設定,用來確認變數有正確代入
  exec <服務> <指令...>   在指定服務的容器內執行指令
  <其他>      原樣傳給 docker compose

範例:
  ./stack.sh up
  ./stack.sh logs backend
  ./stack.sh staging up
  ./stack.sh exec mariadb sh
EOF
}

case "${1:-}" in
    ''|-h|--help|help) usage; exit 0 ;;
esac

if [ ! -f "$ENV_FILE" ]; then
    cat >&2 <<EOF
找不到 $ENV_FILE

請先建立:
  cp ${ENV_FILE}.example ${ENV_FILE}
  chmod 600 ${ENV_FILE}

然後把裡面的 CHANGE_ME 全部換掉:
  密碼      openssl rand -base64 18 | tr -dc 'A-Za-z0-9'
  JWT 金鑰  openssl rand -base64 48
EOF
    exit 1
fi

# 範本裡的 CHANGE_ME 沒換掉的話,容器會以難懂的方式失敗 (例如 MariaDB 起不來,
# 或 JWT 金鑰長度不足) ,不如在這裡直接擋下並指出是哪幾個變數
UNSET_VARS=$(grep -E '^[A-Z_]+=CHANGE_ME$' "$ENV_FILE" | cut -d= -f1 || true)
if [ -n "$UNSET_VARS" ]; then
    echo "$ENV_FILE 還有未填寫的變數:" >&2
    echo "$UNSET_VARS" | sed 's/^/  /' >&2
    echo >&2
    echo "產生方式:  openssl rand -base64 18 | tr -dc 'A-Za-z0-9'   (JWT_SECRET 用 -base64 48)" >&2
    exit 1
fi

CMD=$1; shift

case "$CMD" in
    up)
        # dev 用掛載的原始碼,不需要每次重 build;staging/prod 一律重新 build
        if [ "$ENV_NAME" = dev ]; then
            "${COMPOSE[@]}" up -d "$@"
        else
            "${COMPOSE[@]}" up -d --build "$@"
        fi
        echo
        echo "已啟動.後端 / 資料庫等服務在指令返回時就已通過健康檢查,"
        echo "但前端還要跑 pnpm install 與首次編譯,通常再幾十秒才會有回應:"
        echo "  ./stack.sh ${ENV_NAME} logs frontend"
        ;;
    down)    "${COMPOSE[@]}" down "$@" ;;
    reset)   "${COMPOSE[@]}" down -v "$@" ;;
    restart) "${COMPOSE[@]}" restart "${@:-backend}" ;;
    build)   "${COMPOSE[@]}" build "$@" ;;
    logs)    "${COMPOSE[@]}" logs -f "$@" ;;
    ps)      "${COMPOSE[@]}" ps "$@" ;;
    config)  "${COMPOSE[@]}" config "$@" ;;
    exec)    "${COMPOSE[@]}" exec "$@" ;;
    *)       "${COMPOSE[@]}" "$CMD" "$@" ;;
esac
