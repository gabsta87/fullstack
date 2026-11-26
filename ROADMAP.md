# Roadmap Venus

Source unique des tâches (remplace les anciens `front/TODO` et `serv/TODO`). Conventions : voir `CLAUDE.md`.
Cocher `[x]` quand c'est mergé dans `master`.

---

> Modèle économique : le site loue aux annonceurs la mise en ligne de leur annonce. Il ne gère jamais les rendez-vous ni les échanges entre visiteurs et annonceurs.

## 1. Prêt — petites corrections (commits isolés)

Branche suggérée : `fix/batch-1` (un commit par ligne, merge groupé).

Elements nouveaux

- [ ] Les admins ont encore l'attribut "disponible/indisponible". On pourrait plutôt afficher s'ils sont online ou pas. On peut utiliser la même propriété. Quand ils se connectent, ils deviennent online (disponibles). Quand ils se déconnectent, la valeur est ajustée en conséquence. On pourrait aussi afficher en vert/rouge ce statut pour tout le monde  (disponible/indisponible pour les annonceurs, online/offline pour les admins)

Branche du deuxième lot : `fix/batch-2`.

- [x] message à propos des cookies — *il ne vient pas du site : Venus ne pose aucun cookie (la session est gardée dans le `localStorage` du navigateur, pas dans un cookie), et le code ne contient aucun bandeau ni texte « rester connecté ». Le message « rester connecté sur tous les navigateurs / appareils » est très probablement celui de Chrome (gestionnaire de mots de passe Google, icône de clé dans la barre d'adresse) ou d'une extension. Si tu le revois sur le site, une capture d'écran permettra de trancher. La suggestion `visitor-1234` est reprise pour les commentaires (ligne suivante)*
- [x] front : un visiteur ne peut pas laisser de commentaire s'il n'a pas défini un nom — *chaque visiteur reçoit un pseudonyme `visitor-1234` à l'inscription (jamais tiré de l'e-mail) ; les anciens comptes sans nom en reçoivent un au premier commentaire ; effacer son nom redonne un pseudonyme*
- [x] serv : deux routes de suppression de commentaire par un admin — *elles existaient bien : `DELETE /admin/comments/{id}` et `DELETE /admin/comment/{id}` (singulier). Seule la première est gardée ; pas de PATCH pour supprimer (en REST, PATCH modifie, DELETE supprime). Le front n'a encore aucun écran de modération (`feat/comment-moderation`)*
- [x] avertissement Vite (`The above dynamic import cannot be analyzed`) — *sans danger, uniquement en développement : il vient du chargeur paresseux d'Ionic, embarqué parce que 9 composants importent encore `@ionic/angular` (ancien `IonicModule`) alors que l'appli démarre en mode standalone. Mélanger les deux est déconseillé par Ionic ; la vraie correction est une migration → déplacée en §3 (`chore/ionic-standalone`)*
- [x] front : le formatage du numéro de téléphone avec les espaces est aussi utilisé sur profile-management — *affiché groupé, reformaté en quittant le champ, enregistré sans espaces*
- [x] notification de retour (profil indisponible → disponible) — *vérifié : elle n'existe pas (le bouton « Me notifier son retour » ne fait que changer d'icône, aucun code côté serveur). C'est une fonctionnalité à part entière → déplacée en §2 (`feat/back-online-alert`)*
- [x] front : supprimer la largeur max — *profil, gestion du profil et compte utilisent toute la largeur (la galerie l'utilisait déjà) ; seuls les blocs de texte (description, formulaire de compte, liste de commentaires) gardent une largeur lisible, comme le veut l'usage*
- [x] front : admin-dashboard : les admins ne peuvent pas supprimer un admin / super_admin — *le backend le refusait déjà ; les boutons bannir, verrouiller et supprimer ne sont plus proposés sur ces comptes ni sur son propre compte (règle testée, identique au backend)*
- [x] onglet admin des textes légaux (sélecteur de langue, vue de toutes les langues côte à côte) — *écran complet → fusionné avec `feat/legal-texts-admin` en §2*
- [x] limite globale de stockage modifiable par les admins — *nécessite une table de réglages, un endpoint et un onglet, et touche aux paliers d'abonnement (la limite dépend aujourd'hui du palier, codée en dur) → déplacée en §2 (`feat/admin-settings`)*
- [x] front : spinner pendant le chargement de la liste des utilisateurs (admin)
- [x] front : boutons du formulaire d'avis traduits (« Publier » / « Mettre à jour » restaient en anglais)
- [x] corrigés en vérifiant : la suppression d'un commentaire par un admin échouait toujours sur MySQL (journal d'audit : texte non JSON dans une colonne `json` ; les noms de services/régions supprimés n'étaient pas échappés non plus) — *instantanés sérialisés en vrai JSON, test qui relit chaque instantané* ; une API lente laissait le site blanc au démarrage (restauration de session sans limite de temps) — *4 s au plus*
- [x] phpmyadmin de mon NAS doit être accessible depuis mon réseau local, pas uniquement depuis le NAS, comme c'était le cas avant les modifications que tu as faites. *Port 8082 ouvert sur le réseau local ; ne jamais le rediriger sur la box. Sur un serveur public : `PHPMYADMIN_BIND=127.0.0.1`.*
- [x] front : afficher l'espace utilisé en Mo et % sur la page profile-management pour les images/vidéos — *barre sous le bouton d'ajout, `GET /account/worker/storage`*
- [x] front : le guard profil bloque l'accès à un profil banni (sauf admins) — *un profil non public (404) renvoie à la galerie*
- [x] front : les admins voient quels champs sont manquants pour qu'un profil soit valide — *affichés sous le nom dans la table utilisateurs (`profileIssues` : champs manquants, mineur, expiré) ; la page d'administration d'un profil reste possible plus tard*
- [x] front : les profils favoris sont marqués comme favoris sur la homepage
- [x] serv : sécuriser la demande de certification côté backend (refuser si déjà certifié / en cours de revue) — `chore/tests`
- [x] front+serv : debug — le worker ne voit pas la mise à jour de certification en live (SSE) — *vérifié sur la stack locale : l'annonceur reçoit `CERTIFIED` dès l'approbation*
- [x] test : les admins voient-ils en live les modifications des autres admins (SSE) ? — *non : les événements partaient avant le commit, et le flux n'était pas rouvert après une reconnexion. Corrigé et vérifié (tables utilisateurs et journal mis à jour en direct)*
- [x] front : supprimer la clé `account.favorites.title` de `data.json`
- [x] front : onglets cassés (admin, profile-management) — balises Ionic jamais enregistrées ; corrigé par `ionic-elements.ts` (vérifié en test et en CI)
- [x] front : simplifier les textes afin de gagner de la place (Région, Localisation, Certification — dans les 9 langues)
- [x] serv : nettoyer les noms de tables — *`TableNames` (renommée) couvre toutes les tables ; schéma généré identique, vérifié par `TableNamesTest`*
- [x] front : affichage des espaces pour rendre les numéros de téléphone plus lisibles — *« +41 79 123 45 67 » (libphonenumber-js) ; lien WhatsApp corrigé au passage*
- [x] front : profile management : ajout d'une langue : choix du niveau de maitrise pas encore traduits selon la langue choisie
- [x] front : le bouton escape quand on est dans le champ de recherche par nom doit vider le champ
- [x] front : lors de la première visite du site, un message doit s'afficher pour préciser que le site est réservé à un public adulte (texte légal en DB, plusieurs langues, quitter ou accepter) — *table `legal_text` versionnée par langue, texte initial dans `serv/src/main/resources/legal/`. **Le texte est une proposition : à faire valider (juridiquement) par le propriétaire.** L'acceptation est mémorisée dans le navigateur (version acceptée) ; la modifier passe par `POST /admin/legal` en attendant l'écran admin (`feat/legal-texts-admin`)*
- [x] ajouter la documentation (en anglais) pour tout le projet, concise — *`README.md`, `serv/README.md`, `front/README.md`*
- [x] s'assurer du fonctionnement des SSEStream (admins notifiés des actions des autres admins ; annonceurs notifiés des modifs des admins) — *bannissement → déconnexion immédiate de l'annonceur ; verrouillage, certification → profil mis à jour en direct*
- [x] corrigés en vérifiant : session non restaurée quand on ouvrait directement une page publique (profil affiché comme visiteur) ; photos invisibles sur profile-management en production (`/api/media` → 404)

### Notes laissées pour plus tard

- [x] serv : deux endpoints de suppression de commentaire par un admin font la même chose (`DELETE /admin/comments/{id}` et `/admin/comment/{id}`) : n'en garder qu'un — *fait dans `fix/batch-2`*
- [x] serv : la colonne `legal_text.content` (varchar 255) n'est plus utilisée (remplacée par `body`) : à supprimer à la main (`ALTER TABLE legal_text DROP COLUMN content`) ou avec un outil de migration (Flyway) le jour où il y aura de vraies données
- [ ] admins : les modifications faites par un annonceur sur son propre profil ne rafraîchissent pas la table admin en direct (seulement les actions admin) — à faire si utile, la table étant déjà lourde (`feat/admin-users-table`)
  - REPONSE : PAS BESOIN - par contre, le rafraichissement devra se faire sur la page du profil pour l'administrateur (quand elle sera créée)
- [ ] e-mails : envoyés dans la langue du site au moment de la demande ; un e-mail déclenché plus tard (alerte de ville) devra stocker la langue préférée de l'utilisateur

## 2. Prêt — fonctionnalités (une branche chacune)

| Branche | Sujet | Notes |
|---|---|---|
| `feat/video-upload` | Upload vidéo : compression / limite de taille, stockage, génération de miniature | priorité serveur |
| `feat/account-credentials` | Changer mot de passe / email, protégé par l'ancien mot de passe | |
| `feat/legal-texts-admin` | Onglet admin des textes légaux : modifier ou ajouter une version, historique horodaté (preuve légale) | sélecteur de langue plutôt qu'une ligne par langue ; vue de toutes les langues côte à côte (modale ou page) ; l'API existe déjà (`POST /admin/legal`, versions par langue) |
| `feat/admin-settings` | Onglet « Administration » : limite globale de stockage (photos, vidéos) des annonceurs, plus tard la tarification | **à décider** : une seule limite globale, ou une par palier d'abonnement (aujourd'hui codée en dur dans `Worker.getMaxStorageBytes`) ? Réglages en base, modifiables sans redéploiement |
| `feat/back-online-alert` | « Me notifier son retour » : e-mail aux visiteurs abonnés quand un profil indisponible redevient disponible | table d'abonnements, envoi au passage indisponible → disponible (une fois par retour), lien de désabonnement, langue de l'e-mail stockée (cf. note e-mails ci-dessus). En attendant, le bouton ne fait rien côté serveur |
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
- [x] `chore/english-docs` — commentaires et noms de tests en anglais (~450 commentaires, code inchangé vérifié automatiquement)
  - [x] textes encore en français codés en dur : messages de l'API et journal admin en anglais ; e-mails traduits dans les 9 langues (`messages*.properties`, langue du site envoyée par le front) ; messages du front (`alert`, `confirm`, erreurs) dans `data.json` (section `messages`)
- [x] `chore/logging` — logs SLF4J avec id de requête, événements `SECURITY` / `ADMIN` sans données personnelles, rotation des logs Docker, plus de `console.log` côté front
- [x] `chore/warnings` — zéro warning au démarrage Spring, à la compilation Java (`-Xlint:all` activé en permanence), au build Angular et dans les tests
- [x] `chore/refactoring` — code mort supprimé (composant Stripe, services et resolver inutilisés), Angular Material retiré, CSS globaux chargés une seule fois (plus de Sass `@import`), build Angular moderne (esbuild, 844 → 491 paquets npm) ; rendu vérifié par 30 captures comparées pixel par pixel. Le SCSS des composants n'avait presque pas de duplication : pas de factorisation forcée
- [x] `chore/deps-upgrade` — Spring Boot 3.3 → 4.1.3, Gradle 8.14, Angular 18 → 22, PrimeNG 18 → 22, TypeScript 6, Node 24, MySQL 8.0 → 9.7 LTS ; dépendances inutiles retirées (serveur d'autorisation OAuth2, Thymeleaf, Spring Session, paquets Stripe/Express du front)
  - [x] MySQL 9.7 LTS sur un volume neuf (`db-data-mysql97`) : les données de test repartent de zéro. L'ancien volume peut être supprimé sur le NAS : `docker volume rm venus_db-data`. **Le jour où il y aura de vraies données (Infomaniak)** : sauvegarder avant toute montée MySQL, une LTS à la fois.
  - [ ] `chore/ionic-standalone` : 9 composants importent encore `@ionic/angular` (`IonicModule`, contrôleurs) alors que l'appli démarre en standalone. Migrer vers `@ionic/angular/standalone` (import des composants utilisés), ce qui supprime l'avertissement Vite du chargeur paresseux et, à terme, `ionic-elements.ts`. Comparaison visuelle nécessaire ; à faire avant Ionic 9
  - [ ] Ionic 8 → 9 (majeure, changements visuels possibles) : branche dédiée avec comparaison visuelle
  - [ ] Java 21 → 25 LTS (21 reste supporté jusqu'en 2029 : pas urgent ; nécessite aussi Gradle 9)
  - [x] `@angular/material` retiré (dans `chore/refactoring`)

## 4. Sécurité — suites de l'audit (`chore/tests`)

- [x] visiteurs anonymes bloqués (401 partout, y compris la connexion) quand le navigateur gardait un jeton de l'ancien déploiement : sur les routes publiques un jeton invalide est désormais ignoré (`PublicAccessTest`)
- [x] connexion refusée (403 CORS) quand le site est servi sur un port non standard : nginx transmet l'hôte, le port et le protocole publics

- [x] **Déploiement** : créer le secret GitHub `JWT_SECRET` (≥ 32 caractères, ex. `openssl rand -base64 48`) avant de merger — sinon le déploiement s'arrête
- [x] infra : phpMyAdmin configurable (`PHPMYADMIN_BIND`) : réseau local sur le NAS de staging, `127.0.0.1` + tunnel SSH sur un serveur public
- [x] infra : photos de certification stockées hors de `/media` (volume `private-media`), servies uniquement aux admins par l'API ; migration automatique des anciennes photos au démarrage
- [ ] infra : limitation de débit par IP dans nginx (`limit_req`) pour inscription / login / reset, une fois la vraie IP client connue derrière le reverse proxy Synology
- [x] infra : en-têtes de sécurité nginx (nosniff, X-Frame-Options, Referrer-Policy, CSP sandbox sur `/media`) et `client_max_body_size 110m` — *syntaxe à valider au premier démarrage (`nginx -t`)*
- [ ] infra : CSP sur l'application Angular et HSTS (au niveau du reverse proxy Synology, qui termine le HTTPS) — à tester page par page, Ionic et Stripe utilisant des styles inline
- [x] front : l'intercepteur enregistre le jeton renvoyé dans `X-Auth-Token` et n'envoie plus le jeton qu'à notre API
- [x] décidé : un compte `locked` est un blocage temporaire, il peut se connecter pour y remédier (≠ banni). Vérification d'email avant usage du compte : variable `EMAIL_VERIFICATION_REQUIRED` (false en staging, à mettre à true en production)
- [x] décidé : jetons longs (30 jours, révocables), réglables par `JWT_EXPIRATION` (ex. `P90D`)

## 5. Mise en ligne

- [ ] `chore/deploy-infomaniak` — hébergement Infomaniak
- [ ] `feat/seo` — référencement Google (meta, sitemap, robots.txt, SSR/prerender ?)

---

## 6. À réfléchir (ne pas implémenter)

- **Tri de la galerie** : certifiés d'abord, puis par date de refresh. *La partie sans paiement (certifiés → date) pourrait être faite tout de suite ; refresh payant / cooldown à décider.*
- **Mineurs** : notification quand un utilisateur s'inscrit avec une date de naissance < 18 ans → message aux autorités locales ? Ou impossibilité de créer le compte ? ou bannissement de compte ? Qui, comment, selon quel pays ? (lié à la sécurité des annonceurs mineurs selon les pays)
- **Favoris** les admins et annonceurs ont-ils les droits des visiteurs ? peuvent-ils avoir des favoris ?
- **Admin** Les admins ont-ils besoin de l'onglet compte ? 
- **Certification** : une fois certifié, les photos doivent-elles rester figées ? (seule la suppression est autorisée pour l'instant) pour éviter la fraude ? Peut-être laisse un certain nombre de slots photos libres de changements après certification, genre 2 ou 3.
- **Signalement de numéros de clients** par les annonceurs + outil « Vérifier un numéro » dans l'espace annonceur (plutôt qu'une app mobile)
- **Serveur stateless / montée en charge** : pouvoir lancer plusieurs instances du backend. État actuellement gardé en mémoire à déplacer (DB ou Redis) : flux SSE (`SseStreamService`), limitation des connexions (`AbuseProtectionService`), tâches planifiées (`@Scheduled`, à exécuter sur une seule instance), future file de tarification. Les médias sont sur disque local → stockage partagé (cf. Azure Blob en §7).
- **Super Admin** (sans la partie paiement) : tous les droits admin, ajout/suppression d'admins
- **Statistiques** : nombre de consultations par profil (et par heure)

## 7. Plus tard (site gratuit pour l'instant)

- Système de paiement (Stripe — comment tester avant la mise en prod ?) — *`/payment/**` est fermé (`denyAll`) tant que le site est gratuit ; à rouvrir avec des tests (webhook public mais signé, montant/worker vérifiés côté serveur)*
- Tarification workers : file d'événements persistée en DB pour la consommation de temps (voir détail dans l'ancien `serv/TODO`)
- Abonnements workers (annuel, mensuel…), refresh payant, auto-refresh programmé
- Super Admin : voir les paiements, modifier les tarifications
- Workers : voir leurs paiements et tarifications
- Système de paiement en liquide via employé (quittance, photo, calcul du dû)
- Publicité : suppression des pubs payante (clients / workers, 2 CHF/mois)
- Migration du stockage vers Azure Blob (containers privés, SAS, CDN)
