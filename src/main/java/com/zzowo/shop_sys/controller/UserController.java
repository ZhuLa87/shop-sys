package com.zzowo.shop_sys.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "使用者管理 API")
@RestController
@RequestMapping("/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "取得目前登入的使用者資訊", description = "回傳目前 Token 對應的使用者個人資料")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入或 Token 無效")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal String email) {
        UserResponse userProfile = userService.getUserProfile(email);
        return ResponseEntity.ok(ApiResponse.success("成功通過驗證", userProfile));
    }

    @Operation(summary = "取得特定使用者資訊", description = "依使用者 ID 取得資料 (需要 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "使用者不存在")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "使用者 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal String email) {
        UserResponse userProfile = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("成功取得使用者資料", userProfile));
    }

    @Operation(summary = "取得所有使用者列表", description = "取得系統所有使用者資料 (需要 SUPER_ADMIN 角色) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @Parameter(hidden = true) @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success("成功取得所有使用者資料", userService.getAllUsers()));
    }

    @Operation(summary = "更新自己的個人資料", description = "使用者更新自身資料 (email,密碼,姓名,電話) ;所有欄位皆為選填,留空則不修改")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @Valid @RequestBody UserSelfUpdateRequest request) {
        userService.updateMyInfo(email, request);
        return ResponseEntity.ok(ApiResponse.success("個人資料更新成功"));
    }

    @Operation(summary = "管理員更新特定使用者資料", description = "以 SUPER_ADMIN 身份更新任意使用者資料,包含角色與帳號啟用狀態")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "使用者不存在")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUserByAdmin(
            @Parameter(description = "使用者 ID") @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        userService.updateUserByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("使用者資料更新成功"));
    }
}
