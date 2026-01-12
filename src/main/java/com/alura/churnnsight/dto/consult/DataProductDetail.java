package com.alura.churnnsight.dto.consult;

import com.alura.churnnsight.model.Product;

public record DataProductDetail(
        String productName

) {
    public DataProductDetail(Product product){
        this(
                product.getName()
        );
    }
}
