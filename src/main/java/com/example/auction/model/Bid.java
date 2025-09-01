package com.example.auction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="product_id", nullable=false)
    private Product product;

    @ManyToOne
    @JoinColumn(name="buyer_id", nullable=false)
    private User buyer;

    @Column(nullable=false)
    private BigDecimal amount;

    @Column(nullable=false)
    private LocalDateTime bidTime;

    // Constructors, getters & setters
    public Bid() {}
 // Bid.java
    public Bid(User buyer, Product product, BigDecimal amount) {
        this.buyer = buyer;
        this.product = product;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
    }
    
    @PrePersist
    public void onCreate() {
        this.bidTime = LocalDateTime.now();
    }


    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User c) { this.buyer = c; }

    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime t) { this.bidTime = t; }
}

