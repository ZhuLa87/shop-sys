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
        res.setQuantity(cart.getQuantity());

        // CartService 已在上游過濾 null product,此處僅作防禦性保護
        var product = cart.getProduct();
        if (product != null) {
            res.setProductId(product.getId());
            res.setProductName(product.getName());
            res.setCoverImageUrl(product.getCoverImageUrl());
            res.setPrice(product.getPrice());
            res.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        return res;
    }
}