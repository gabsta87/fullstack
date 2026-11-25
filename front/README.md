# Venus front

Angular 22 (standalone components, `@angular/build` application builder), Ionic 8, PrimeNG 22 (admin tables).
Node 24 (`.nvmrc`). In production the build is served by nginx (`nginx.conf`), which also proxies `/api` to the backend.

```bash
npm start                                              # dev server, API on http://<host>:8080 (environment.ts)
npx ng build --configuration production                # must stay at 0 warning
npx ng test --watch=false --browsers=ChromeHeadless    # Karma + Jasmine
```

## Layout (`src/app`)

| Folder | Content |
|---|---|
| `features/components` | Pages (homepage gallery, profile, account, profile-management, admin-dashboard) and modals |
| `features/services` | API access and state: `AuthService` (session, account SSE stream), `AdminService` (admin SSE stream), `LanguageManagerService` (translations) |
| `features/guards`, `features/resolver` | Route protection and data loaded before a page opens |
| `features/interceptors` | `JwtInterceptor`: adds the token and the site language to API requests only |
| `features/utils` | Small pure helpers (input limits, phone formatting, error-message mapping), unit-tested |
| `ionic-elements.ts` | Registers every `<ion-*>` tag used in templates (a CI check fails if one is missing) |

## Rules

- **Texts**: every UI text goes through `src/assets/lang/data.json` (9 languages). Templates use the current
  translation object `t`; code uses `languageService.msg('key', { param })` (`messages.*` section). Server error texts
  are never shown as is: map the HTTP status to a message key (see `utils/error-messages.ts`).
- **Security**: a check done in the front (disabled button, guard) must also exist in the backend. The token is only
  sent to our API (`isApiRequest`).
- **Logs**: no `console.log` (refused by the CI); `console.warn` / `console.error` only, in English.
- **Styles**: global CSS (Ionic, Swiper) is loaded once from `angular.json`; `theme/variables.scss` holds the design
  tokens; component styles stay in their component.

## Live updates

`AuthService` keeps `/account/stream` open while logged in (account updates, `session-expired` on ban or deletion).
`AdminService` opens `/admin/stream` for admins and reloads the matching list on each event (users, certifications,
reports, services, regions, audit log).
