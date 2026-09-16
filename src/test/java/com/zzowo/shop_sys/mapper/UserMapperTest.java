package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toUserResponse_mapsEnabledFlag() {
        // 後台會員管理靠這個欄位顯示啟用狀態,漏掉會讓所有帳號一律顯示成停用
        assertThat(userMapper.toUserResponse(buildUser(true)).getEnabled()).isTrue();
        assertThat(userMapper.toUserResponse(buildUser(false)).getEnabled()).isFalse();
    }

    @Test
    void toUserResponse_mapsBasicFields() {
        UserResponse response = userMapper.toUserResponse(buildUser(true));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getName()).isEqualTo("測試使用者");
        assertThat(response.getRole()).isEqualTo("SUPER_ADMIN");
        assertThat(response.getPhone()).isEqualTo("0912345678");
    }

    @Test
    void toUserResponse_nullUser_returnsNull() {
        assertThat(userMapper.toUserResponse(null)).isNull();
    }

    private User buildUser(boolean enabled) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setName("測試使用者");
        user.setPhone("0912345678");
        user.setRole(Role.SUPER_ADMIN);
        user.setEnabled(enabled);
        return user;
    }
}
