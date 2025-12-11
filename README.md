# 🏷️ Application d’Enchères (Auction App)

Une application web d’enchères en ligne développée avec **Spring Boot**, **Thymeleaf**, **Spring Security**, et **Spring Data JPA**.

Les utilisateurs peuvent :
- Créer un compte (venduer ou acheteur)
- Publier des produits à enchérir avec une image
- Rechercher et filtrer les produits par nom ou catégorie
- Placer des enchères jusqu’à une date d’expiration fixe (3 jours)
- Voir la liste des produits et les détails d’un produit

---

## 🧱 Fonctionnalités principales

✔️ Authentification (login/signup + rôles SELLER / BUYER)  
✔️ Upload d’images pour les produits  
✔️ Recherche par mot-clé + filtre par catégorie  
✔️ Gestion des enchères jusqu’à une durée fixe (3 jours)  
✔️ Suppression automatique des produits expirés  
✔️ Navigation organisée avec header/footer fragmentés  
✔️ Pagination simple (facultatif à implémenter si souhaité)

---

## 📁 Structure du projet

application-d-enchere/
├── src/
│ ├── main/
│ │ ├── java/com/example/auction/
│ │ │ ├── AuctionAppApplication.java
│ │ │ ├── config/
│ │ │ │ ├── SecurityConfig.java
│ │ │ │ ├── WebMvcConfig.java
│ │ │ │ └── FileStorageConfig.java
│ │ │ ├── controller/
│ │ │ │ ├── AuthController.java
│ │ │ │ ├── ProductController.java
│ │ │ │ └── BidController.java
│ │ │ ├── model/
│ │ │ │ ├── User.java
│ │ │ │ ├── Product.java
│ │ │ │ └── Bid.java
│ │ │ ├── repository/
│ │ │ └── service/
│ │ ├── resources/
│ │ │ ├── application.properties
│ │ │ └── templates/
│ │ │ ├── fragments/
│ │ │ │ ├── header.html
│ │ │ │ └── footer.html
│ │ │ ├── login.html
│ │ │ ├── signup.html
│ │ │ ├── products.html
│ │ │ ├── product-detail.html
│ │ │ └── add-product.html
└── pom.xml


## 🔧 Prérequis

Avant d’installer :

- **Java 17+**
- **Maven**
- **MySQL** (ou autre base de données compatible JPA)

---

## ⚙️ Configuration (application.properties)

properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/auctiondb?serverTimezone=UTC
spring.datasource.username=ton_user
spring.datasource.password=ton_mdp
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Upload d’images
file.upload-dir=uploads

🚀 Installation & lancement

    Clone ton repo :

git clone https://github.com/mehdi-zgui/application-d-enchere.git
cd application-d-enchere

    Compile et lance l’app :

mvn clean install
mvn spring-boot:run

    Ouvre dans ton navigateur :

👉 http://localhost:8080/products
📌 Points techniques

🔹 Upload d’images

Les images sont stockées dans un dossier local uploads/, servi en ressources statiques grâce à :

registry.addResourceHandler("/images/**")
        .addResourceLocations("file:" + uploadDir + "/");

Et affichées dans Thymeleaf :

<img th:src="@{/images/{fn}(fn=${product.imageUrl})}" />


🔹 Enchères + Fin fixe

La date de fin est définie à Instant.now().plus(3 jours) lors de l’ajout.


🔹 Suppression automatique des produits expirés

Une tâche planifiée (@Scheduled) supprime les produits expirés :

@Scheduled(cron = "0 0 2 * * *")
public void purgeExpiredAuctions() {
    Instant now = Instant.now();
    productRepository.deleteByEndDateBefore(now);
}

🔹 Formatage des dates dans Thymeleaf

Dans le controller détail :

String formatted = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(product.getEndDate());

m.addAttribute("endDateFormatted", formatted);


🧠 Bonnes pratiques

✔️ Préférer Instant pour la gestion des dates en backend (UTC)
✔️ Formulaires bien configurés avec enctype="multipart/form-data"
✔️ Sécurité Spring Security pour les rôles et protection CSRF
✔️ Thymeleaf fragments pour header/footer uniformes


🧪 Test manuel

  Enregistrer un vendeur puis ajouter un produit avec image

  Consulter la liste, rechercher par mot-clé ou catégorie

  Voir la page détail et vérifier l’image + date formatée

  Attendre 3 jours ou changer endDate manuellement pour tester la purge


📄 Licence

Projet personnel — à adapter selon ton choix si tu veux partager publiquement.
