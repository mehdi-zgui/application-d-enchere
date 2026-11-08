# API REST d'enchères

Backend Java 17 / Spring Boot 3.5.16, Spring Security et JPA/MySQL.
Les contrôleurs renvoient des DTO JSON ; les entités JPA restent internes.
Les vues Thymeleaf et les anciennes routes HTML ont été supprimées.

## Démarrage

Prérequis : Java 17+, Maven, MySQL et le schéma existant.
Configurer DB_URL, DB_USERNAME et DB_PASSWORD dans l'environnement.
DB_URL vaut par défaut jdbc:mysql://localhost:3306/auction_db?serverTimezone=UTC.
Aucun mot de passe de base n'est fourni par défaut.

~~~sh
mvn test
mvn spring-boot:run
~~~

Les tests utilisent H2 et ne nécessitent pas MySQL. Pour une base locale vide,
on peut démarrer avec --spring.jpa.hibernate.ddl-auto=update ; en production,
gérer explicitement les migrations de schéma.
Avant une exposition publique, configurer le proxy pour limiter le débit des
requêtes, notamment sur l'inscription et la connexion.
UPLOAD_DIR configure le stockage des images (uploads par défaut).
En production, utiliser HTTPS et SESSION_COOKIE_SECURE=true.

## Authentification

Sessions HTTP (cookie JSESSIONID, expiration après 30 minutes) avec CSRF activé.
Le client conserve les cookies et récupère GET /api/auth/csrf avant les écritures.
Envoyer la valeur token dans l'en-tête indiqué par headerName.
Récupérer un nouveau token après connexion et après déconnexion.
Aucun JWT n'est nécessaire. Le frontend doit partager l'origine de l'API
(par exemple via un reverse proxy) ; CORS n'est pas ouvert par défaut.

| Méthode | Route | Corps / résultat |
| --- | --- | --- |
| GET | /api/auth/csrf | Objet token et headerName ; accessible sans connexion |
| POST | /api/auth/signup | JSON username, password, role ; 201 avec utilisateur |
| POST | /api/auth/login | Formulaire URL-encoded username/password ; 200 JSON |
| GET | /api/auth/me | Utilisateur connecté |
| POST | /api/auth/logout | 204 et invalidation de session |

L'inscription accepte seulement BUYER ou SELLER. Le mot de passe comporte de
8 à 72 caractères ; BCrypt le hache. Les DTO n'exposent jamais son hash.
Une inscription ne connecte pas automatiquement l'utilisateur.
Le login garde le format standard de Spring Security :
Content-Type: application/x-www-form-urlencoded.

## Produits et enchères

Toutes ces routes exigent une connexion.

| Méthode | Route | Accès / comportement |
| --- | --- | --- |
| GET | /api/products?keyword=...&category=... | Recherche, tableau JSON |
| GET | /api/products/{id} | Détail, 404 si absent |
| POST | /api/products | SELLER ; 201 avec en-tête Location |
| GET | /api/products/{id}/bids | Enchères triées par montant décroissant |
| POST | /api/products/{id}/bids | BUYER ; corps vide ; 201 avec enchère |
| GET | /images/{filename} | Image du produit |

Exemple de corps JSON pour créer un produit :

~~~json
{
  "title": "Ordinateur",
  "description": "Très bon état",
  "initialPrice": 100.00,
  "category": "INFORMATIQUE"
}
~~~

La catégorie est un texte libre de 255 caractères maximum (par exemple : Horlogerie).
Le vendeur et la date de fin (dans trois jours, UTC) sont fixés côté serveur.
Pour joindre une image, envoyer multipart/form-data avec une partie product
de type application/json contenant le même objet et une partie imageFile.
PNG et JPEG uniquement, 5 Mo maximum, 20 mégapixels maximum.
Les images sont décodées, réencodées en PNG et enregistrées sous un nom aléatoire.

Chaque enchère augmente le prix courant de 10 %, arrondi au centime supérieur.
Un verrou pessimiste sur le produit sérialise les enchères concurrentes.
Les enchères expirées sont refusées (409), et un vendeur ne peut pas enchérir
sur son propre produit. La purge horaire existante est conservée ;
auction.purge.enabled=false permet de la désactiver.

Les erreurs utilisent application/problem+json :
400 données invalides, 401 connexion requise, 403 droits/CSRF,
404 ressource absente, 409 conflit, 413 fichier trop volumineux.
Les erreurs de validation contiennent une liste errors (field, message).

## Exemple curl

Conserver le fichier cookies.txt entre les commandes ; copier le token renvoyé
dans TOKEN (puis le renouveler après la connexion).

~~~sh
curl -c cookies.txt http://localhost:8080/api/auth/csrf
curl -b cookies.txt -c cookies.txt -H "X-CSRF-TOKEN: TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"vendeur","password":"password123","role":"SELLER"}' \
  http://localhost:8080/api/auth/signup
curl -b cookies.txt -c cookies.txt -H "X-CSRF-TOKEN: TOKEN" \
  --data-urlencode "username=vendeur" --data-urlencode "password=password123" \
  http://localhost:8080/api/auth/login
curl -b cookies.txt -c cookies.txt http://localhost:8080/api/auth/csrf
curl -b cookies.txt -H "X-CSRF-TOKEN: NOUVEAU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Ordinateur","initialPrice":100,"category":"INFORMATIQUE"}' \
  http://localhost:8080/api/products
~~~
