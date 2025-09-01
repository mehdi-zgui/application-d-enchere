package com.example.auction.controller;

import com.example.auction.model.Product;
import com.example.auction.model.User;
import com.example.auction.service.ProductService;
import com.example.auction.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService ps;
    private final UserService us;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public ProductController(ProductService ps, UserService us) {
        this.ps = ps;
        this.us = us;
    }

    @GetMapping
    public String list(@RequestParam(required=false) String keyword,
                       @RequestParam(required=false) String category,
                       Model m) {
        m.addAttribute("products", ps.search(keyword, category));
        m.addAttribute("keyword", keyword);
        m.addAttribute("category", category);
        return "products";
    }

    @GetMapping("/add")
    public String addForm(Model m) {
        m.addAttribute("product", new Product());
        return "add-product";
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String addSubmit(@ModelAttribute Product product,
                            @RequestParam("imageFile") MultipartFile file,
                            @AuthenticationPrincipal UserDetails ud) throws Exception {
        // Upload de l’image
        if (!file.isEmpty()) {
            String filename = System.currentTimeMillis() + "_"
                              + Paths.get(file.getOriginalFilename()).getFileName();
            Path dest = Paths.get(uploadDir).resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            product.setImageUrl(filename);
        }
        // Assignation du vendeur
        User seller = us.findByUsername(ud.getUsername()).orElseThrow();
        product.setSeller(seller);
        // Fin de l’enchère dans 3 jours (UTC)
        Instant nowUtc = Instant.now();
        product.setEndDate(nowUtc.plus(3, ChronoUnit.DAYS));
        ps.save(product);
        return "redirect:/products";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        // 1) Récupère le produit
        Product product = ps.findById(id);

        // 2) Le met dans le modèle
        m.addAttribute("product", product);

        // 3) Formate l’Instant en chaîne « dd/MM/yyyy HH:mm » dans le fuseau système
        String formatted = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(product.getEndDate());
        m.addAttribute("endDateFormatted", formatted);

        return "product-detail";
    }
}

