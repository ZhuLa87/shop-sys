# stack.sh 的薄包裝,給習慣打 make 的人用.
# 實際邏輯全在 ./stack.sh,兩邊行為一致 (make 未安裝時直接用 ./stack.sh 即可) .

.PHONY: help
help:
	@./stack.sh --help

# ── dev ──────────────────────────────────────────────────────────────────────
.PHONY: dev-up dev-down dev-reset dev-restart dev-build dev-logs dev-ps dev-config
dev-up:      ; @./stack.sh dev up
dev-down:    ; @./stack.sh dev down
dev-reset:   ; @./stack.sh dev reset
dev-restart: ; @./stack.sh dev restart
dev-build:   ; @./stack.sh dev build
dev-logs:    ; @./stack.sh dev logs
dev-ps:      ; @./stack.sh dev ps
dev-config:  ; @./stack.sh dev config

# ── staging ──────────────────────────────────────────────────────────────────
.PHONY: staging-up staging-down staging-build staging-logs staging-ps staging-config
staging-up:     ; @./stack.sh staging up
staging-down:   ; @./stack.sh staging down
staging-build:  ; @./stack.sh staging build
staging-logs:   ; @./stack.sh staging logs
staging-ps:     ; @./stack.sh staging ps
staging-config: ; @./stack.sh staging config

# ── prod ─────────────────────────────────────────────────────────────────────
.PHONY: prod-up prod-down prod-build prod-logs prod-ps prod-config
prod-up:     ; @./stack.sh prod up
prod-down:   ; @./stack.sh prod down
prod-build:  ; @./stack.sh prod build
prod-logs:   ; @./stack.sh prod logs
prod-ps:     ; @./stack.sh prod ps
prod-config: ; @./stack.sh prod config
