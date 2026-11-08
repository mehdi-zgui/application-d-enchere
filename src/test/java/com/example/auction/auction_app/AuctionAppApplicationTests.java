package com.example.auction.auction_app;

import com.example.auction.model.*;
import com.example.auction.repository.*;
import com.example.auction.service.BidService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auction;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "auction.purge.enabled=false",
    "file.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class AuctionAppApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired BidRepository bids;
    @Autowired BidService bidService;

    @BeforeEach void reset() {
        bids.deleteAll(); products.deleteAll(); users.deleteAll();
    }

    void signup(String name, String role) throws Exception {
        mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json")
            .content("{\"username\":\"" + name + "\",\"password\":\"password123\",\"role\":\"" + role + "\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test void authenticationAndCsrf() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(post("/api/auth/signup").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        signup("buyer", "BUYER");
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","buyer").param("password","wrong"))
            .andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/api/auth/login").with(csrf())
            .param("username","buyer").param("password","password123"))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("buyer")).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void validationAndDuplicateNames() throws Exception {
        mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json")
            .content("{\"username\":\"\",\"password\":\"x\",\"role\":\"BUYER\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json")
            .content("{\"username\":\"admin\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest());
        signup("buyer","BUYER");
        mvc.perform(post("/api/auth/signup").with(csrf()).contentType("application/json")
            .content("{\"username\":\"buyer\",\"password\":\"password123\",\"role\":\"BUYER\"}"))
            .andExpect(status().isConflict());
    }

    @Test void realCsrfTokenWorksAcrossLogin() throws Exception {
        var initial = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) initial.getRequest().getSession(false);
        String token = json.readTree(initial.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/signup").session(session).header("X-CSRF-TOKEN", token)
            .contentType("application/json")
            .content("{\"username\":\"session-user\",\"password\":\"password123\",\"role\":\"BUYER\"}"))
            .andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN", token)
            .param("username","session-user").param("password","password123"))
            .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) login.getRequest().getSession(false);
        var refreshed = mvc.perform(get("/api/auth/csrf").session(session))
            .andExpect(status().isOk()).andReturn();
        String newToken = json.readTree(refreshed.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", newToken))
            .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void productAndBidLifecycle() throws Exception {
        signup("seller","SELLER"); signup("buyer","BUYER");
        String input = "{\"title\":\"Laptop\",\"description\":\"Test\",\"initialPrice\":100,\"category\":\"INFORMATIQUE\"}";
        mvc.perform(post("/api/products").with(user("buyer").roles("BUYER")).with(csrf())
            .contentType("application/json").content(input)).andExpect(status().isForbidden());
        var result = mvc.perform(post("/api/products").with(user("seller").roles("SELLER")).with(csrf())
            .contentType("application/json").content(input)).andExpect(status().isCreated())
            .andExpect(header().exists("Location")).andExpect(jsonPath("$.seller.password").doesNotExist()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/products").param("keyword","laptop").param("category","INFORMATIQUE")
            .with(user("buyer"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/products/" + id).with(user("buyer"))).andExpect(status().isOk());
        mvc.perform(post("/api/products/" + id + "/bids").with(user("seller").roles("SELLER")).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/products/" + id + "/bids").with(user("buyer").roles("BUYER")).with(csrf()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(110));
        mvc.perform(post("/api/products/" + id + "/bids").with(user("buyer").roles("BUYER")).with(csrf()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(121));
        mvc.perform(get("/api/products/" + id + "/bids").with(user("buyer")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].buyer.password").doesNotExist());
        Product product = products.findById(id).orElseThrow();
        product.setEndDate(Instant.now().minusSeconds(1)); products.save(product);
        mvc.perform(post("/api/products/" + id + "/bids").with(user("buyer").roles("BUYER")).with(csrf()))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/products/999999").with(user("buyer"))).andExpect(status().isNotFound());
    }

    @Test void concurrentBidsHaveDistinctAmounts() throws Exception {
        signup("seller","SELLER"); signup("buyer","BUYER");
        Product p = products.save(new Product("Laptop","",new BigDecimal("100.00"),null,
            "INFORMATIQUE",Instant.now().plusSeconds(3600),users.findByUsername("seller").orElseThrow()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<BigDecimal> place = () -> { start.await(); return bidService.place(p.getId(),"buyer").amount(); };
            Future<BigDecimal> first = executor.submit(place);
            Future<BigDecimal> second = executor.submit(place);
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(new BigDecimal("110.00"), new BigDecimal("121.00"));
        } finally { executor.shutdownNow(); }
    }

    @Test void freeFormCategoryCanBeCreatedAndSearched() throws Exception {
        signup("seller","SELLER");
        mvc.perform(post("/api/products").with(user("seller").roles("SELLER")).with(csrf())
            .contentType("application/json")
            .content("{\"title\":\"Montre\",\"initialPrice\":50,\"category\":\" Horlogerie \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.category").value("Horlogerie"));
        mvc.perform(get("/api/products").param("category","horlogerie").with(user("buyer")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Montre"));
    }

    @Test void purgeDeletesOnlyExpiredProductsAndTheirBids() throws Exception {
        signup("seller","SELLER"); signup("buyer","BUYER");
        var seller = users.findByUsername("seller").orElseThrow();
        Product expired = products.save(new Product("Ancien","",BigDecimal.TEN,null,
            "Art",Instant.now().minusSeconds(1),seller));
        Product active = products.save(new Product("Ouvert","",BigDecimal.TEN,null,
            "Art",Instant.now().plusSeconds(3600),seller));
        bids.save(new Bid(users.findByUsername("buyer").orElseThrow(), expired, BigDecimal.TEN));
        new com.example.auction.service.AuctionPurgeService(products).purgeExpiredAuctions();
        assertThat(products.existsById(expired.getId())).isFalse();
        assertThat(products.existsById(active.getId())).isTrue();
        assertThat(bids.count()).isZero();
    }

    @Test void multipartImageAndInvalidImage() throws Exception {
        signup("seller","SELLER");
        var product = new org.springframework.mock.web.MockMultipartFile("product", "", "application/json",
            "{\"title\":\"Phone\",\"initialPrice\":50,\"category\":\"TELEPHONIE\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",output);
        var image = new org.springframework.mock.web.MockMultipartFile("imageFile","../../image.png","image/png",output.toByteArray());
        var result = mvc.perform(multipart("/api/products").file(product).file(image)
            .with(user("seller").roles("SELLER")).with(csrf()))
            .andExpect(status().isCreated()).andReturn();
        String imageUrl = json.readTree(result.getResponse().getContentAsString()).get("imageUrl").asText();
        mvc.perform(get(imageUrl).with(user("seller"))).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("image/png"));
        var invalid = new org.springframework.mock.web.MockMultipartFile("imageFile","fake.png","image/png","invalid".getBytes());
        mvc.perform(multipart("/api/products").file(product).file(invalid)
            .with(user("seller").roles("SELLER")).with(csrf())).andExpect(status().isBadRequest());
    }
}
