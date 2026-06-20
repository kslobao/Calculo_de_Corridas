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

# Docker (preferred)
docker-compose up -d

# Scheduled jobs
php artisan schedule:run
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
  database/      ← Room entities, DAOs, AppDatabase
  network/       ← Retrofit ApiService, DTOs, AuthInterceptor
  repositories/  ← RideRepositoryImpl, SelectorRepositoryImpl, etc.
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

`AppSelectors` contains typed pattern lists: `pricePatterns`, `distancePatterns`, `timePatterns`, `originPatterns`, `destinationPatterns`, `categoryPatterns`. Each `Selector` has a `type` (ACCESSIBILITY_ID / REGEX / CONTENT_DESC) and `priority`.

## Monetization Model

- **Free plan**: AdMob banner + interstitial (frequency via Firebase Remote Config), history limited to 7 days.
- **PRO plan**: Google Play Billing subscriptions (`pro_monthly`, `pro_yearly`), also manual licenses via backend (`gift` / `partner` / `beta` / `admin` sources).
- `BillingManager` owns the `BillingClient` lifecycle; `LicenseValidator` holds a `StateFlow<License>` consumed throughout the app.
- License check: `POST /api/v1/license/check` with `SHA256(ANDROID_ID + packageName)` as device token.

## Backend API (Laravel 11)

REST JSON API under `/api/v1/` consumed by the Android app:
- `POST /api/v1/devices/register` — device registration, returns auth token
- `GET  /api/v1/selectors?version=N` — returns selector config (304 if up to date)
- `POST /api/v1/license/check` — validate license / subscription
- `POST /api/v1/subscription/validate` — Google Play RTDN webhook handler
- `POST /api/v1/parser/report` — crowdsourced parser failure reports

Admin panel at `/admin` (Blade views) behind JWT auth.

Key backend services: `LicenseService`, `SelectorService`, `GooglePlayService` (Play Developer API), `RtdnService` (real-time developer notifications).

## Sensitive Values

Never commit real values for:
- `admob_app_id` in `strings.xml` (use test ID `ca-app-pub-3940256099942544~3347511713` for debug)
- `app/google-services.json` — obtain from Firebase Console
- Keystore files and `keystore.properties`
- Backend `.env` (copy from `.env.example`)
