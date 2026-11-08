package com.example.auction.controller;

import com.example.auction.dto.ApiDtos.*;
import com.example.auction.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;
    public ProductController(ProductService products) { this.products = products; }

    @GetMapping
    public List<ProductView> list(@RequestParam(required=false) String keyword,
                                  @RequestParam(required=false) String category) {
        return products.search(keyword, category);
    }

    @GetMapping("/{id}")
    public ProductView detail(@PathVariable Long id) { return products.detail(id); }

    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductView> create(@Valid @RequestBody ProductInput input, Authentication auth) {
        return created(products.create(input, null, auth.getName()));
    }

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductView> createWithImage(@Valid @RequestPart("product") ProductInput input,
            @RequestPart(value="imageFile", required=false) MultipartFile image, Authentication auth) {
        return created(products.create(input, image, auth.getName()));
    }

    private ResponseEntity<ProductView> created(ProductView product) {
        return ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product);
    }
}
