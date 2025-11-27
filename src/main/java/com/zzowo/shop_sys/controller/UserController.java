package com.zzowo.shop_sys.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;


@RestController // 告訴 Spring這是一個 REST API 控制器 (回傳 JSON)
@RequestMapping("/user") // 設定此控制器的基礎路徑
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser() {
        // 回傳目前登入的使用者資訊 (這裡先回傳簡單字串，實務上會回傳 User DTO)
        return ResponseEntity.ok("成功通過驗證");
    }
}
