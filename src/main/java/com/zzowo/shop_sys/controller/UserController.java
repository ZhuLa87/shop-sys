package com.zzowo.shop_sys.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController // 告訴 Spring這是一個 REST API 控制器 (回傳 JSON)
@RequestMapping("/v1/users") // 設定此控制器的基礎路徑
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 取得目前登入的使用者資訊
     * URL: GET /v1/users/me
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {

        UserResponse userProfile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("成功通過驗證", userProfile));
    }

    /**
     * 超級管理員取得特定使用者資訊
     * @param userDetails
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse userProfile = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("成功取得使用者資料", userProfile));
    }

    /**
     * 超級管理員取得所有使用者資訊
     * URL: GET /v1/users
     * @param userDetails
     * @param request
     * @return
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success("成功取得所有使用者資料", userService.getAllUsers()));
    }

    /**
     * 一般使用者更新自己
     * URL: PUT /v1/users/me
     * @param userDetails
     * @param request
     * @return
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserSelfUpdateRequest request) {

        userService.updateMyInfo(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("個人資料更新成功"));
    }

    /**
     * 超級管理員更新特定用戶
     * URL: PUT /v1/users/{id}
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUserByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {

        userService.updateUserByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("使用者資料更新成功"));
    }
}
