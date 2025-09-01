package com.example.auction.service;

import com.example.auction.model.Product;
import com.example.auction.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public Product save(Product p) {
        return repo.save(p);
    }

    public Product findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public List<Product> activeAuctions() {
        // Vous pouvez filtrer endDate > now(), ou renvoyer tout
        return repo.findAll();
    }

    public List<Product> search(String keyword, String category) {
        String k = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String c = (category == null || category.isBlank()) ? null : category.trim();
        return repo.search(k, c);
    }
}
