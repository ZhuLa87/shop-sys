package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // 告訴 Spring這是一個 REST API 控制器 (回傳 JSON)
@RequestMapping("/auth") // 設定此控制器的基礎路徑
public class AuthController {

    @Autowired
    private UserService userService;

    // 註冊 API
    // URL: POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegisterRequest request) {
        // 呼叫 Service 執行業務邏輯 (檢查 Email、加密密碼、存檔)
        User newUser = userService.register(request);

        // 回傳簡單的成功訊息 (實務上也可以回傳 User DTO)
        return ResponseEntity.ok("註冊成功！您的會員 ID 為: " + newUser.getId());
    }

    // 登入 API (先預留位置，下一階段實作 JWT 時會用到)
    @PostMapping("/login")
    public ResponseEntity<String> login() {
        return ResponseEntity.ok("登入功能開發中...");
    }
}