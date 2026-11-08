package com.example.auction.service;

import com.example.auction.dto.ApiDtos.*;
import com.example.auction.model.Product;
import com.example.auction.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly=true)
public class ProductService {
    private final ProductRepository repo;
    private final UserService users;
    private final ImageStorageService images;
    public ProductService(ProductRepository repo, UserService users, ImageStorageService images) {
        this.repo = repo; this.users = users; this.images = images;
    }
    public Product findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
    }
    public ProductView detail(Long id) { return ProductView.from(findById(id)); }
    public List<ProductView> search(String keyword, String category) {
        String k = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String c = category == null || category.isBlank() ? null : category.trim();
        return repo.search(k, c).stream().map(ProductView::from).toList();
    }
    @Transactional
    public ProductView create(ProductInput input, MultipartFile image, String username) {
        var seller = users.require(username);
        Product product = new Product(input.title().trim(), input.description(), input.initialPrice(),
            images.store(image), input.category().trim(), Instant.now().plus(3, ChronoUnit.DAYS),
            seller);
        return ProductView.from(repo.save(product));
    }
}
