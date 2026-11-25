# Roadmap Venus

Regroupe `front/TODO` et `serv/TODO`. Conventions : voir `CLAUDE.md`.
Cocher `[x]` quand c'est mergé dans `master`.

---

## 1. Prêt — petites corrections (commits isolés)

Branche suggérée : `fix/batch-1` (un commit par ligne, merge groupé).

- [ ] front : afficher l'espace utilisé sur la page profile-management
- [ ] front : le guard profil bloque l'accès à un profil banni (sauf admins) — *backend fait dans `chore/tests` : `GET /workers/{id}` répond 404 pour un profil non public, sauf admin/propriétaire*
- [ ] front : les admins voient tous les champs requis de chaque profil dans le tableau (détecter les problèmes de validité)
- [ ] front : les profils favoris sont marqués comme favoris sur la homepage
- [x] serv : sécuriser la demande de certification côté backend (refuser si déjà certifié / en cours de revue) — `chore/tests`
- [ ] front+serv : debug — le worker ne voit pas la mise à jour de certification en live (SSE)
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

## 3. Chantiers transverses (branches `chore/…`)

À faire tôt : ils facilitent tout le reste.

- [ ] `chore/tests` — mise en place des tests : non-régression, bugs existants, cas non standards / sécurité
  - [x] serv : infrastructure d'intégration (H2, vrais JWT), matrice d'autorisation, auth/jetons, uploads, validation, IDOR, droits admin — 327 tests
  - [ ] front : tests Angular (`ng test`) — services d'auth, guards, formulaires
  - [ ] CI : lancer `./gradlew test` dans le workflow avant le déploiement
- [ ] `chore/english-docs` — documentation en anglais, remplacement des commentaires en français
- [ ] `chore/logging` — logs minimaux mais clairs (SLF4J, retirer les `println`, niveaux cohérents)

## 4. Sécurité — suites de l'audit (`chore/tests`)

- [ ] **Déploiement** : créer le secret GitHub `JWT_SECRET` (≥ 32 caractères, ex. `openssl rand -base64 48`) avant de merger — sinon le déploiement s'arrête
- [ ] infra : phpMyAdmin est exposé sur le port 8082 → le retirer du compose de prod ou le limiter au réseau local / VPN
- [ ] infra : photos de certification servies publiquement par nginx (`/media/{id}/certification/…`, URL non devinable mais publique) → les stocker hors de `/media` et les servir via un endpoint admin
- [ ] infra : limitation de débit par IP dans nginx (`limit_req`) pour inscription / login / reset, une fois la vraie IP client connue derrière le reverse proxy Synology
- [ ] infra : en-têtes de sécurité nginx (`X-Content-Type-Options: nosniff` sur `/media`, CSP, HSTS) et `client_max_body_size` cohérent avec les limites d'upload (50 Mo)
- [ ] front : lire l'en-tête `X-Auth-Token` renvoyé après un changement de mot de passe / email (les anciens jetons sont révoqués) — à faire avec `feat/account-credentials`
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
- **Super Admin** (sans la partie paiement) : tous les droits admin, page AdminAuditLog filtrable (admin, date, type, raison), ajout/suppression d'utilisateurs et admins
- **Statistiques** : nombre de consultations par profil (et par heure)
- **UI** : mini-carte de profil au survol au lieu de lancer le carrousel

## 7. Plus tard (site gratuit pour l'instant)

- Système de paiement (Stripe — comment tester avant la mise en prod ?) — *`/payment/**` est fermé (`denyAll`) tant que le site est gratuit ; à rouvrir avec des tests (webhook public mais signé, montant/worker vérifiés côté serveur)*
- Tarification workers : file d'événements persistée en DB pour la consommation de temps (voir détail dans l'ancien `serv/TODO`)
- Abonnements workers (annuel, mensuel…), refresh payant, auto-refresh programmé, gestion de RDV
- Super Admin : voir les paiements, modifier les tarifications
- Workers : voir leurs paiements et tarifications
- Système de paiement en liquide via employé (quittance, photo, calcul du dû)
- Publicité : suppression des pubs payante (clients / workers, 2 CHF/mois)
- Migration du stockage vers Azure Blob (containers privés, SAS, CDN)
