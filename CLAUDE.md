# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app for ride-hailing drivers (Uber, 99, inDrive, iFood) that monitors ride offers in real time via `AccessibilityService` and shows a floating overlay with calculated metrics — no root required. Commercial app targeting Google Play Store.

The repo has two components:
- `app/` — Android app (Kotlin)
- `backend/` — Laravel 11 API + admin panel (PHP)

## Android Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release AAB (for Play Store)
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.calculocorridas.SomeTestClass"

# Lint
./gradlew lint
```

**Prerequisites:** Android Studio Hedgehog+, JDK 17, Android SDK 35, `app/google-services.json` from Firebase Console.

**Build variants:** `debug` uses `applicationId = "com.calculocorridas.debug"` and hits `https://api-dev.calculocorridas.com/`; `release` hits `https://api.calculocorridas.com/`.

Signing: create `keystore.properties` at the repo root and add a `signingConfigs` block in `app/build.gradle.kts`.

## Backend Commands

```bash
cd backend
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate --seed
php artisan serve          # dev server on :8000

# Scheduled jobs
php artisan schedule:run
```

## Docker (Production — Docker Swarm)

Two images must be built and pushed:

```bash
cd backend/

# PHP-FPM image
docker build -t registry.hostdatec.com.br/calc-corridas-app:v1.0.0 \
  -f docker/php/Dockerfile .
docker push registry.hostdatec.com.br/calc-corridas-app:v1.0.0

# Nginx image (config baked in — no Docker config object needed)
docker build -t registry.hostdatec.com.br/calc-corridas-nginx:v1.0.0 \
  -f docker/nginx/Dockerfile docker/nginx/
docker push registry.hostdatec.com.br/calc-corridas-nginx:v1.0.0
```

Deploy via Portainer: Stacks → Add Stack → paste `stack.yml` → load `.env` → Deploy.

After deploy, run migrations:

```bash
docker exec -it $(docker ps -qf "name=calculo_de_corridas_app" | head -1) \
  php artisan migrate --seed --force
```

**Container name pattern in Swarm:** `<stack>_<service>.<replica>.<task_id>`  
Filter: `docker ps -qf "name=calculo_de_corridas_app"` (stack name is `app_calculo_de_corridas` in Portainer).

**Logs:** `LOG_CHANNEL=stderr` — Laravel logs go to stderr. Use `docker service logs -f app_calculo_de_corridas_app` or Portainer container logs. No `storage/logs/laravel.log` in production.

**APP_KEY generation:**
```bash
docker run --rm registry.hostdatec.com.br/calc-corridas-app:v1.0.0 php artisan key:generate --show
```

## Android Architecture

**Clean Architecture + MVVM + Repository Pattern** with Hilt DI.

```
domain/          ← pure Kotlin, no Android deps
  entities/      ← Ride, Rule, AppSource, VehicleProfile, License
  engine/        ← RideCalculationEngine, RuleEngine
  repositories/  ← interfaces only
  usecases/      ← one class per use case

data/            ← implements domain interfaces
  DeviceRegistrar.kt   ← handles POST /api/v1/device/register (lazy, once per install)
  database/      ← Room entities, DAOs, AppDatabase
  network/       ← Retrofit ApiService, DTOs, AuthInterceptor
  repositories/  ← RideRepositoryImpl, SelectorRepositoryImpl, LicenseRepositoryImpl
  billing/       ← BillingManager (Google Play Billing)
  ads/           ← AdManager, ConsentManager (AdMob UMP)
  remoteconfig/  ← RemoteConfigManager (Firebase)

services/
  accessibility/ ← RideAccessibilityService + per-app parsers
  overlay/       ← OverlayService (floating window) + OverlayView
  watchdog/      ← WatchdogService, BootReceiver

selectors/       ← SelectorConfigHolder (in-memory cache), PatternMatcher
workers/         ← SelectorSyncWorker (6h), LicenseSyncWorker
licensing/       ← LicenseValidator (singleton state)
di/              ← Hilt modules: App, Database, Network, Repository, UseCase
```

## Core Data Flow

1. `RideAccessibilityService.onAccessibilityEvent()` fires on `TYPE_WINDOW_STATE_CHANGED` / `TYPE_WINDOW_CONTENT_CHANGED` for the four monitored package names.
2. Events are debounced 300 ms, then routed to the correct `BaseParser` subclass (Uber/99/inDrive/iFood).
3. `BaseParser.parse()` uses `PatternMatcher` to walk the accessibility tree using `AppSelectors` fetched from `SelectorConfigHolder`.
4. `CurrencyParser`, `DistanceParser`, `TimeParser` convert raw strings to typed values.
5. `RideCalculationEngine` computes `valuePerKm`, `valuePerHour`, `fuelCost`, `netProfit` from the parsed data and the active `VehicleProfile`.
6. `RuleEngine.evaluate()` classifies the ride as EXCELLENT / GOOD / POOR by running the user's enabled rules by priority; falls back to `valuePerKm` thresholds (≥2.50 = excellent, ≥2.00 = good).
7. The `Ride` is saved to Room and the classification + metrics are sent to `OverlayService` via `Intent` extras.

## Remote Selectors (critical design decision)

Selectors are **never hardcoded** in the app. They are fetched from `GET /api/v1/selectors?version=N` and cached in Room (`SelectorCacheEntity`) and in memory (`SelectorConfigHolder`). `SelectorSyncWorker` refreshes every 6 hours. This allows updating extraction patterns when driver apps change their UI without publishing a new APK.

`AppSelectors` contains typed pattern lists: `price_patterns`, `distance_patterns`, `time_patterns`, `origin_patterns`, `destination_patterns`, `category_patterns`. Each `SelectorPattern` has a `type` (`accessibility_id` / `regex` / `content_desc` / `class_name` — **always lowercase**) and `priority`.

> ⚠️ Selector type values are **lowercase** in both DB and wire format. The `SelectorType.fromKey()` enum matches lowercase strings. The backend `SelectorType` enum uses lowercase values. The PostgreSQL CHECK constraint (`chk_selectors_selector_type`) also enforces lowercase — never insert uppercase values.

## Android API Contract

All requests include `Authorization: Bearer <SHA256(ANDROID_ID+packageName)>` plus `X-App-Version` and `X-Platform: android` headers (via `AuthInterceptor`).

### Endpoint: `POST /api/v1/device/register` — **must be called first**

Handled by `DeviceRegistrar.ensureRegistered()`, which is called lazily from both `LicenseRepositoryImpl.checkRemote()` and `SelectorRepositoryImpl.getRemote()`. Registration is persisted in DataStore; subsequent calls are no-ops.

```json
// Request body
{ "device_token": "<SHA256>", "package_name": "com.calculocorridas", "app_version": "1.0.0" }

// Response 201
{ "registered": true, "device_id": "<uuid>", "plan": "free" }
```

### Endpoint: `GET /api/v1/selectors?version=N`

Returns `304 Not Modified` if client version is current. Response structure:

```json
{
  "version": 2,
  "updated_at": "2026-06-20T00:00:00Z",
  "apps": {
    "uber": {
      "price_patterns":       [{ "type": "accessibility_id", "value": "com.ubercab.driver:id/trip_fare", "priority": 100 }],
      "distance_patterns":    [{ "type": "regex",            "value": "([\\d.,]+)\\s*km",               "priority": 50  }],
      "time_patterns":        [],
      "origin_patterns":      [],
      "destination_patterns": [],
      "category_patterns":    []
    },
    "99":      { ... },
    "indrive": { ... },
    "ifood":   { ... }
  }
}
```

### Endpoint: `POST /api/v1/license/check`

```json
// Request — only purchase_token needed (device identified via Bearer header)
{ "purchase_token": "tok_xyz_or_null" }

// Response
{
  "active": true,
  "plan": "pro",
  "source": "google",
  "expires_at": "2026-12-31T23:59:59Z",
  "features": {
    "ads_free": true,
    "unlimited_history": true,
    "cloud_backup": true,
    "export": true,
    "multi_vehicle": true
  }
}
```

### Endpoint: `POST /api/v1/subscription/validate` — purchase & restore flow

Called by `LicenseValidator.validateAndActivate()` after a successful Google Play purchase or restore. Validates directly against Google Play Developer API.

```json
// Request
{ "product_id": "pro_monthly", "purchase_token": "tok_xyz" }

// Response 200 — subscription valid
{
  "valid": true,
  "plan": "pro",
  "source": "google",
  "expires_at": "2026-12-31T23:59:59Z",
  "features": { "ads_free": true, "unlimited_history": true, ... }
}

// Response 422 — subscription invalid or expired
{ "valid": false, "reason": "subscription_invalid_or_expired" }
```

### Endpoint: `POST /api/v1/parser/report`

```json
// Request
{
  "app_key": "uber",
  "selector_version": 2,
  "success": false,
  "raw_texts": ["R$ 18,50", "3,2 km"],
  "error_message": "Pattern not matched: price",
  "app_version": "1.0.0"
}
```

### Restore Purchases Flow

```
User taps "Restaurar compras"
        │
        ▼
SubscriptionViewModel.restorePurchases()
        │
        ▼
BillingManager.queryPurchases()
        │ (Google Play returns existing purchase)
        ▼
BillingManager.state → SubscriptionState.Subscribed(productId, purchaseToken)
        │
        ▼
LicenseValidator.validateAndActivate(productId, purchaseToken)
        │
        ▼
POST /api/v1/subscription/validate
        │
        ├── 200 valid → License saved to DataStore → UI shows PRO
        └── 422 invalid → fallback: POST /api/v1/license/check
```

## Monetization Model

- **Free plan**: AdMob banner + interstitial (frequency via Firebase Remote Config), history limited to 7 days.
- **PRO plan**: Google Play Billing subscriptions (`pro_monthly`, `pro_yearly`), also manual licenses via backend (`gift` / `partner` / `beta` / `admin` sources).
- `BillingManager` owns the `BillingClient` lifecycle; `LicenseValidator` holds a `StateFlow<License>` consumed throughout the app.
- New purchase → `SubscriptionState.Subscribed` → `LicenseValidator.validateAndActivate()` → `POST /api/v1/subscription/validate`.
- Restore purchases → same flow via `BillingManager.queryPurchases()`.

## Backend API (Laravel 11)

REST JSON API under `/api/v1/` consumed by the Android app. All protected routes require `Authorization: Bearer <device_token>`.

| Method | Route | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/device/register` | none | Device registration (bootstrap) |
| `GET`  | `/api/v1/selectors?version=N` | Bearer | Remote selectors (304 if current) |
| `POST` | `/api/v1/license/check` | Bearer | License validation |
| `POST` | `/api/v1/subscription/validate` | Bearer | Google Play subscription validate/restore |
| `POST` | `/api/v1/parser/report` | Bearer | Parser failure report |
| `GET`  | `/api/v1/config` | Bearer | Public app config |
| `POST` | `/api/v1/google/rtdn` | token query | Google Pub/Sub RTDN webhook |
| `GET`  | `/health` | none | Service health |

Admin panel at `/admin` (Blade views) behind JWT auth (cookie `admin_token`).

Health endpoints: `GET /up` (Laravel), `GET /nginx-health` (static 200, no PHP-FPM dependency).

Key backend services: `LicenseService`, `SelectorService`, `GooglePlayService` (Play Developer API), `RtdnService` (real-time developer notifications).

**Docker architecture:** Traefik (edge, 80/443) → Nginx custom image (internal only, port 80) → PHP-FPM (port 9000). Nginx config is baked into `calc-corridas-nginx` image via `docker/nginx/Dockerfile`. `SCRIPT_FILENAME` uses hardcoded `/var/www/html/public$fastcgi_script_name` because `$realpath_root` resolves on the Nginx container where app files don't exist.

## License Priority (backend)

```
POST /api/v1/license/check
  1. Manual active license (gift/partner/beta/admin) → return PRO immediately
  2. purchase_token sent → validate with Google Play → save + return PRO
  3. Existing Google license in DB → return PRO
  4. Default → return FREE
```

Subscriptions belong to the **user**, not the device. One purchase covers all devices with the same `user_id`.

## Sensitive Values

Never commit real values for:
- `admob_app_id` in `strings.xml` (use test ID `ca-app-pub-3940256099942544~3347511713` for debug)
- `app/google-services.json` — obtain from Firebase Console
- Keystore files and `keystore.properties`
- Backend `.env` (copy from `.env.example`)
- `GOOGLE_SERVICE_ACCOUNT_JSON` — Google Play service account credentials
