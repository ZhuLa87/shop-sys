package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.entity.Cart;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CartMapper {

    public CartItemResponse toCartItemResponse(Cart cart) {
        CartItemResponse res = new CartItemResponse();
        res.setId(cart.getId());
        res.setProductId(cart.getProduct().getId());
        res.setProductName(cart.getProduct().getName());
        res.setCoverImageUrl(cart.getProduct().getCoverImageUrl());
        res.setPrice(cart.getProduct().getPrice());
        res.setQuantity(cart.getQuantity());
        // 計算小計
        res.setSubtotal(cart.getProduct().getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        return res;
    }
}