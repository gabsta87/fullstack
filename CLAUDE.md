# CLAUDE.md — Venus

Ce fichier est lu automatiquement par Claude au début de chaque session.
Il décrit le projet, les conventions et la façon de travailler ensemble.

## Le projet

- **front/** : Angular 22 + Ionic 8 (+ PrimeNG 22), Node 24 (`front/.nvmrc`), servi par nginx.
- **serv/** : Spring Boot 4.1 (Java 21, Gradle 8.14 Kotlin DSL), package `com.serv`.
- **DB** : MySQL 9.7 LTS (`docker-compose.yml`, `ddl-auto: update`). Montées de version MySQL : une LTS à la fois, sauvegarde avant.
- **Déploiement** : `.github/workflows/deploy.yml` — **chaque push sur `master` déploie sur le staging** (runner self-hosted sur le NAS).
- Pour l'instant **le site est gratuit** : ne rien développer autour du paiement (Stripe, tarification, achat de temps) sans demande explicite.

## Commandes utiles

```bash
cd front && npm start          # dev server Angular (proxy.conf.json)
cd front && npm run build
cd front && npm test
cd serv && ./gradlew bootRun
cd serv && ./gradlew test
```

### Tester la stack complète en local (comme sur le NAS)

Docker Desktop doit être lancé. Créer une fois `.env.local` à partir de `.env.example` (valeurs locales, jamais commitées), puis :

```bash
docker compose -p venus-local --env-file .env.local up -d --build   # site sur http://localhost:8888
docker logs -f venus-backend                                        # logs du backend
docker compose -p venus-local --env-file .env.local down            # arrêt (les données restent dans les volumes)
```

### Logs

- Backend : SLF4J uniquement (un test interdit `System.out` / `printStackTrace`). Chaque ligne porte l'id de requête (`[a1b2c3d4]`, aussi renvoyé dans l'en-tête `X-Request-Id`).
- Événements de sécurité préfixés `SECURITY`, actions admin préfixées `ADMIN` : `docker logs venus-backend 2>&1 | grep -E "SECURITY|ADMIN"`.
- Jamais de mot de passe, jeton ou email dans les logs (vérifié par `LoggingTest`) : identifier les comptes par leur id.
- Front : pas de `console.log` (refusé par la CI), seulement `console.warn` / `console.error`.

## Organisation du travail

La liste des tâches est dans **`ROADMAP.md`** (source de vérité, remplace les fichiers TODO).
Chaque tâche y est marquée avec un type :

- **[branche]** : fonctionnalité ou chantier conséquent → une branche dédiée.
- **[commit]** : petite correction → un commit isolé, regroupés sur une branche `fix/…`.
- **[à réfléchir]** : ne pas implémenter, en attente de décision.
- **[plus tard]** : hors périmètre pour l'instant (paiement, etc.).

### Git

- `master` déploie automatiquement sur le **staging** (NAS, https://venustest.synology.me) ; la production (Infomaniak) n'existe pas encore.
- **Claude ne pousse jamais** : il peut merger dans `master` en local, le push reste fait à la main par le propriétaire.
- Avant de proposer un push, la check-list « Avant de pousser » ci-dessous doit être entièrement verte. Le suivi de la CI se fait avec `gh run list` / `gh run watch`, le staging se vérifie depuis la machine locale.
- Noms de branches : `feat/<sujet>`, `fix/<sujet>`, `chore/<sujet>` (tests, docs, logs, nettoyage).
- Une branche = un sujet. Commits intermédiaires petits et cohérents (une étape qui compile).
- Format des messages de commit (convention existante) : `front : …`, `serv : …`, `docker : …`, `ci : …`.
  Exemple : `serv : add old-password check on email change`.
### Avant de proposer un push sur `master` (check-list obligatoire)

1. `cd serv && ./gradlew test` : tous les tests passent, aucun warning de compilation.
2. `cd front && npx ng build --configuration production` (0 warning) et `npx ng test --watch=false --browsers=ChromeHeadless`.
3. Stack Docker locale reconstruite (`docker compose -p venus-local --env-file .env.local up -d --build`) : démarrage du backend sans WARN/ERROR, parcours de base vérifié (pages publiques en anonyme, inscription, connexion d'un client et de l'admin, une action par rôle).
4. Si l'interface a changé : vérification visuelle dans le navigateur des pages touchées.
5. Après le push (fait par le propriétaire) : suivre la CI (`gh run watch`), vérifier le staging (pages publiques, connexion) et lire les logs du NAS si besoin (`ssh venus-nas-logs logs venus-backend 300`).
- Ne jamais committer `.env`, secrets, `node_modules`, `dist`, `build`.

### Quand Claude prend une tâche de la roadmap

1. Créer la branche indiquée depuis `master` à jour.
2. Découper en commits intermédiaires.
3. Cocher la tâche dans `ROADMAP.md` (dans le dernier commit de la branche).
4. S'arrêter avant le merge / push et résumer ce qui a été fait.
5. Si la tâche cache une décision produit non tranchée, demander plutôt que deviner.

## Tests (backend)

- Tests d'intégration : étendre `serv/src/test/java/com/serv/support/IntegrationTest` (vrai contexte Spring, vraie chaîne de sécurité, vrais JWT, H2 vidée avant chaque test, dossier média temporaire). Pas de `@Transactional` sur les tests.
- **Tout nouvel endpoint** doit être ajouté à `security/AuthorizationMatrixTest` (anonyme / client / worker / admin).
- Toute nouvelle entrée utilisateur : limites dans `common/InputLimits`, annotations Bean Validation + `@Valid`, et un test des cas invalides (vide, trop long, mauvais format, id d'un autre utilisateur).
- Une entrée invalide doit donner **4xx, jamais 500** ; `GlobalExceptionHandler` ne renvoie jamais de détails internes.
- Les mots de passe passent par `PasswordPolicy`, les emails par `Email` (normalisés en minuscules).
- Les fichiers uploadés passent par `MediaStorageService` (type détecté par contenu, images ré-encodées).

## Conventions de code

- Code, commentaires et documentation **en anglais** (les anciens commentaires en français sont remplacés au fil de l'eau).
- Pas de `System.out.println` côté serveur : utiliser le logger (SLF4J).
- Toute vérification de sécurité faite côté front (bouton désactivé, guard) doit aussi exister côté backend.
- Les textes UI passent par `front/src/assets/lang/data.json` (traductions).
