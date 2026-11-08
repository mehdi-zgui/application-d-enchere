package com.example.auction.dto;

import com.example.auction.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {}
    public record Signup(@NotBlank @Size(max=100) String username,
                         @NotBlank @Size(min=8, max=72) String password,
                         @NotNull Role role) {}
    public enum Role { BUYER, SELLER }
    public record UserView(Long id, String username, String role) {
        public static UserView from(User user) {
            return new UserView(user.getId(), user.getUsername(), user.getRole());
        }
    }
    public record ProductInput(@NotBlank @Size(max=255) String title,
                               @Size(max=1000) String description,
                               @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal initialPrice,
                               @NotBlank @Size(max=255) String category) {}
    public record ProductView(Long id, String title, String description, BigDecimal initialPrice,
                              String category, String imageUrl, Instant endDate, UserView seller) {
        public static ProductView from(Product p) {
            return new ProductView(p.getId(), p.getTitle(), p.getDescription(), p.getInitialPrice(),
                p.getCategory(), p.getImageUrl() == null ? null : "/images/" + p.getImageUrl(),
                p.getEndDate(), UserView.from(p.getSeller()));
        }
    }
    public record BidView(Long id, Long productId, UserView buyer, BigDecimal amount, LocalDateTime bidTime) {
        public static BidView from(Bid b) {
            return new BidView(b.getId(), b.getProduct().getId(), UserView.from(b.getBuyer()),
                b.getAmount(), b.getBidTime());
        }
    }
}
