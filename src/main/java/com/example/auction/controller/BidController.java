package com.example.auction.controller;

import com.example.auction.model.Bid;
import com.example.auction.model.Product;
import com.example.auction.model.User;
import com.example.auction.service.BidService;
import com.example.auction.service.ProductService;
import com.example.auction.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/bids")
public class BidController {
    private final BidService bs;
    private final ProductService ps;
    private final UserService us;
    public BidController(BidService bs, ProductService ps, UserService us) {
        this.bs = bs; this.ps = ps; this.us = us;
    }

    @PostMapping("/place/{productId}")
    public String place(@PathVariable Long productId,
                        @AuthenticationPrincipal UserDetails ud,
                        RedirectAttributes redirectAttributes) {
        Product p = ps.findById(productId);
        User buyer = us.findByUsername(ud.getUsername()).get();
        BigDecimal current = p.getBids().stream()
                              .map(Bid::getAmount)
                              .max(BigDecimal::compareTo)
                              .orElse(p.getInitialPrice());
        BigDecimal next = current.multiply(BigDecimal.valueOf(1.1));
        try {
            Bid bid = new Bid();
            bid.setProduct(p);
            bid.setBuyer(buyer);
            bid.setAmount(next);
            bid.setBidTime(LocalDateTime.now());
            bs.save(bid);
            redirectAttributes.addFlashAttribute("success", "Enchère placée avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l’enregistrement de l’enchère.");
        }
        return "redirect:/bids/" + productId;
    }


    @GetMapping("/{productId}")
    public String list(@PathVariable Long productId, Model m) {
        Product p = ps.findById(productId);
        m.addAttribute("bids", bs.findByProduct(p));
        m.addAttribute("product", p);
        return "bids";
    }
}
