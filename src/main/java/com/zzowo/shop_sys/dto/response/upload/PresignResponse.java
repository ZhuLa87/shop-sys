package com.zzowo.shop_sys.dto.response.upload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignResponse {

    private String uploadUrl;
    private String publicUrl;
    private String objectKey;
}
