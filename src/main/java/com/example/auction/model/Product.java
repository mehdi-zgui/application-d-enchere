package com.example.auction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String title;

    @Column(length=1000)
    private String description;

    // stocke le filename
    private String imageUrl;

    @Column(nullable=false)
    private BigDecimal initialPrice;

    @Column(nullable = false)
    private String category;

    @Column(nullable=false)
    private Instant  endDate;

    @ManyToOne
    @JoinColumn(name="seller_id", nullable=false)
    private User seller;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Bid> bids;

    public Product() {}

    public Product(String title, String description, BigDecimal initialPrice,
                   String imageUrl, String category, Instant  endDate, User seller) {
        this.title = title;
        this.description = description;
        this.initialPrice = initialPrice;
        this.imageUrl = imageUrl;
        this.category = category;
        this.endDate = endDate;
        this.seller = seller;
    }

    // getters & setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public BigDecimal getInitialPrice() { return initialPrice; }
    public void setInitialPrice(BigDecimal p) { this.initialPrice = p; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String url) { this.imageUrl = url; }

    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }

    public Instant  getEndDate() { return endDate; }
    public void setEndDate(Instant  dt) { this.endDate = dt; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public List<Bid> getBids() { return bids; }
    public void setBids(List<Bid> b) { this.bids = b; }
}

