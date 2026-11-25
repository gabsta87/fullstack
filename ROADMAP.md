# Roadmap Venus

Source unique des tâches (remplace les anciens `front/TODO` et `serv/TODO`). Conventions : voir `CLAUDE.md`.
Cocher `[x]` quand c'est mergé dans `master`.

---

> Modèle économique : le site loue aux annonceurs la mise en ligne de leur annonce. Il ne gère jamais les rendez-vous ni les échanges entre visiteurs et annonceurs.

## 1. Prêt — petites corrections (commits isolés)

Branche suggérée : `fix/batch-1` (un commit par ligne, merge groupé).

- [ ] front : afficher l'espace utilisé sur la page profile-management
- [ ] front : le guard profil bloque l'accès à un profil banni (sauf admins) — *backend fait dans `chore/tests` : `GET /workers/{id}` répond 404 pour un profil non public, sauf admin/propriétaire*
- [ ] front : les admins voient tous les champs requis de chaque profil dans le tableau (détecter les problèmes de validité)
- [ ] front : les profils favoris sont marqués comme favoris sur la homepage
- [x] serv : sécuriser la demande de certification côté backend (refuser si déjà certifié / en cours de revue) — `chore/tests`
- [ ] front+serv : debug — le worker ne voit pas la mise à jour de certification en live (SSE) — *cause probable corrigée dans `chore/tests` (le front lisait un rôle absent du JWT) ; à vérifier en conditions réelles*
- [ ] test : les admins voient-ils en live les modifications des autres admins (SSE) ?
- [ ] front : supprimer la clé `account.favorites.title` de `data.json`
- [ ] front : simplifier les textes pour les régions (au lieu de "choisir une région" -> Région / localisation) afin de gagner de la place
- [ ] serv : nettoyer les noms de tables (`@Table(name = …)`) en corrigeant et utilisant la classe TablesNames
- [ ] s'assurer du fonctionnement des SSEStream pour que les admins soient notifiés en temps réel des actions des autres admins. Ainsi que les annonceurs notifiés des modifs des amins sur leur propre profile-management 

## 2. Prêt — fonctionnalités (une branche chacune)

| Branche | Sujet | Notes |
|---|---|---|
| `feat/video-upload` | Upload vidéo : compression / limite de taille, stockage, génération de miniature | priorité serveur |
| `feat/account-credentials` | Changer mot de passe / email, protégé par l'ancien mot de passe | |
| `feat/legal-texts-admin` | Les admins modifient les textes légaux + historique horodaté (preuve légale) | |
| `feat/comment-moderation` | Signalement de commentaires par les annonceurs + modération admin | |
| `feat/age-certification` | Certification de l'âge par les admins | voir aussi §6 (mineurs) |
| `feat/certification-expiration` | Expiration automatique des certifications (table triée par date, réveil à l'échéance) | |
| `feat/city-alert-email` | Alerte email quand un profil arrive dans la ville souhaitée | nécessite un service d'envoi d'email |
| `feat/gallery-cache` | Cache pour réduire les requêtes de la galerie | après les tests, idéalement |
| `feat/photo-management-ui` | Gestion des photos plus jolie et moderne (les boutons actuels sont peu esthétiques) | profile-management |
| `feat/admin-users-table` | Alléger la table utilisateurs de l'admin-dashboard (lente à charger) sans perdre le filtrage sur l'ensemble des utilisateurs | piste : pagination + filtrage côté serveur (endpoint paginé `GET /admin/users?page&size&q&role&status`), DTO de liste allégé (détails chargés à l'ouverture d'une ligne), index SQL sur les colonnes filtrées |
| `feat/clear-filters` | Croix / touche Échap pour annuler le filtrage courant | galerie et tables admin |
| `feat/availability-calendar` | Les annonceurs affichent leurs jours de disponibilité à venir dans un calendrier, visible par les clients sur leur profil | affichage seul : les RDV ne sont jamais gérés par le site, visiteur et annonceur s'arrangent entre eux |

## 3. Chantiers transverses (branches `chore/…`)

À faire tôt : ils facilitent tout le reste.

- [x] `chore/tests` — mise en place des tests : non-régression, bugs existants, cas non standards / sécurité
  - [x] serv : infrastructure d'intégration (H2, vrais JWT), matrice d'autorisation, auth/jetons, uploads, validation, IDOR, droits admin, photos de certification privées — 334 tests
  - [x] front : tests Angular (`ng test`) — intercepteur JWT, AuthService (SSE inclus), guards, formulaire de connexion, utilitaires, photo de certification — 51 tests
  - [x] CI : tests backend + front sur GitHub avant tout déploiement (et sur chaque pull request)
- [ ] `chore/english-docs` — documentation en anglais, remplacement des commentaires en français
- [x] `chore/logging` — logs SLF4J avec id de requête, événements `SECURITY` / `ADMIN` sans données personnelles, rotation des logs Docker, plus de `console.log` côté front
- [x] `chore/warnings` — zéro warning au démarrage Spring, à la compilation Java (`-Xlint:all` activé en permanence), au build Angular et dans les tests
- [ ] `chore/refactoring` — nettoyage, factorisation du code, en particulier des fichiers SCSS (variables, mixins, styles partagés)
- [ ] `chore/deps-upgrade` — mise à jour des dépendances pour la sécurité et les performances : MySQL 8.0 → 9.7 LTS (latest), Angular 18 → latest, Ionic, Spring Boot 3.3 → dernière 3.x, Stripe, etc. *À faire après `chore/tests` : les tests servent de filet de sécurité. Une montée de version par commit.*

## 4. Sécurité — suites de l'audit (`chore/tests`)

- [ ] **Déploiement** : créer le secret GitHub `JWT_SECRET` (≥ 32 caractères, ex. `openssl rand -base64 48`) avant de merger — sinon le déploiement s'arrête
- [x] infra : phpMyAdmin n'écoute plus que sur `127.0.0.1:8082` du NAS (accès via tunnel SSH)
- [x] infra : photos de certification stockées hors de `/media` (volume `private-media`), servies uniquement aux admins par l'API ; migration automatique des anciennes photos au démarrage
- [ ] infra : limitation de débit par IP dans nginx (`limit_req`) pour inscription / login / reset, une fois la vraie IP client connue derrière le reverse proxy Synology
- [x] infra : en-têtes de sécurité nginx (nosniff, X-Frame-Options, Referrer-Policy, CSP sandbox sur `/media`) et `client_max_body_size 110m` — *syntaxe à valider au premier démarrage (`nginx -t`)*
- [ ] infra : CSP sur l'application Angular et HSTS (au niveau du reverse proxy Synology, qui termine le HTTPS) — à tester page par page, Ionic et Stripe utilisant des styles inline
- [x] front : l'intercepteur enregistre le jeton renvoyé dans `X-Auth-Token` et n'envoie plus le jeton qu'à notre API
- [ ] à décider : un compte `locked` (email non vérifié) peut encore se connecter ; faut-il bloquer le login tant que l'email n'est pas vérifié ?
- [ ] à décider : durée de vie des jetons (30 jours actuellement, révocables)

## 5. Mise en ligne

- [ ] `chore/deploy-infomaniak` — hébergement Infomaniak
- [ ] `feat/seo` — référencement Google (meta, sitemap, robots.txt, SSR/prerender ?)

---

## 6. À réfléchir (ne pas implémenter)

- **Tri de la galerie** : certifiés d'abord, puis par date de refresh. *La partie sans paiement (certifiés → date) pourrait être faite tout de suite ; refresh payant / cooldown à décider.*
- **Mineurs** : notification quand un utilisateur s'inscrit avec une date de naissance < 18 ans → message aux autorités locales ? Qui, comment, selon quel pays ? (lié à la sécurité des annonceurs mineurs selon les pays)
- **Régions** générées selon le pays (et non par les utilisateurs)
- **Certification** : une fois certifié, les photos doivent-elles rester figées (seul l'ajout autorisé) pour éviter la fraude ?
- **Signalement de numéros de clients** par les annonceurs + outil « Vérifier un numéro » dans l'espace annonceur (plutôt qu'une app mobile)
- **Serveur stateless / montée en charge** : pouvoir lancer plusieurs instances du backend. État actuellement gardé en mémoire à déplacer (DB ou Redis) : flux SSE (`SseStreamService`), limitation des connexions (`AbuseProtectionService`), tâches planifiées (`@Scheduled`, à exécuter sur une seule instance), future file de tarification. Les médias sont sur disque local → stockage partagé (cf. Azure Blob en §7).
- **Super Admin** (sans la partie paiement) : tous les droits admin, page AdminAuditLog filtrable (admin, date, type, raison), ajout/suppression d'utilisateurs et admins
- **Statistiques** : nombre de consultations par profil (et par heure)
- **UI** : mini-carte de profil au survol au lieu de lancer le carrousel

## 7. Plus tard (site gratuit pour l'instant)

- Système de paiement (Stripe — comment tester avant la mise en prod ?) — *`/payment/**` est fermé (`denyAll`) tant que le site est gratuit ; à rouvrir avec des tests (webhook public mais signé, montant/worker vérifiés côté serveur)*
- Tarification workers : file d'événements persistée en DB pour la consommation de temps (voir détail dans l'ancien `serv/TODO`)
- Abonnements workers (annuel, mensuel…), refresh payant, auto-refresh programmé
- Super Admin : voir les paiements, modifier les tarifications
- Workers : voir leurs paiements et tarifications
- Système de paiement en liquide via employé (quittance, photo, calcul du dû)
- Publicité : suppression des pubs payante (clients / workers, 2 CHF/mois)
- Migration du stockage vers Azure Blob (containers privés, SAS, CDN)
