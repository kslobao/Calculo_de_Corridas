# Correções Aplicadas — Android ↔ Backend

**Data:** 2026-06-20  
**Baseado em:** `ANDROID_BACKEND_COMPATIBILITY_REPORT.md`

---

## Fase 1 — Correções Críticas

---

### C1 — Registro de dispositivo (Android)

**Problema:** App nunca chamava `POST /api/v1/device/register`, causando 401 em todos os endpoints protegidos.

**Arquivos criados:**

| Arquivo | Conteúdo |
|---|---|
| `app/.../data/network/dto/DeviceDto.kt` | `DeviceRegisterRequest`, `DeviceRegisterResponse` |
| `app/.../data/DeviceRegistrar.kt` | Singleton com `ensureRegistered()` — lazy, idempotente via DataStore |

**Arquivos modificados:**

| Arquivo | Mudança |
|---|---|
| `app/.../data/network/ApiService.kt` | Adicionado `registerDevice()` |
| `app/.../data/repositories/LicenseRepositoryImpl.kt` | Injeta `DeviceRegistrar`; `checkRemote()` chama `ensureRegistered()` primeiro |
| `app/.../data/repositories/SelectorRepositoryImpl.kt` | Injeta `DeviceRegistrar`; `getRemote()` chama `ensureRegistered()` primeiro |

**Comportamento:** Registro acontece uma vez por instalação. DataStore persiste o flag `device_registered`. Reinstalação invalida automaticamente.

---

### C2 — Chaves de campo dos seletores (backend)

**Problema:** `FieldType.androidKey()` retornava camelCase; Android esperava snake_case via `@SerializedName`.

**Arquivo modificado:** `backend/app/Enums/FieldType.php`

| Antes | Depois |
|---|---|
| `'pricePatterns'` | `'price_patterns'` |
| `'distancePatterns'` | `'distance_patterns'` |
| `'timePatterns'` | `'time_patterns'` |
| `'originPatterns'` | `'origin_patterns'` |
| `'destinationPatterns'` | `'destination_patterns'` |
| `'categoryPatterns'` | `'category_patterns'` |

---

### C3 — Tipo de seletor uppercase vs lowercase (backend + DB)

**Problema:** `SelectorType` enum usava UPPERCASE; `SelectorType.fromKey()` do Android esperava lowercase.

**Arquivos modificados:**

| Arquivo | Mudança |
|---|---|
| `backend/app/Enums/SelectorType.php` | Valores: `'ACCESSIBILITY_ID'` → `'accessibility_id'`, etc. |
| `backend/database/seeders/SelectorVersionSeeder.php` | Seeds com lowercase |

**Arquivo criado:**

| Arquivo | Conteúdo |
|---|---|
| `backend/database/migrations/2024_01_02_000001_lowercase_selector_types.php` | `UPDATE selectors SET selector_type = LOWER(selector_type)` |

> Rodar `php artisan migrate` em produção para atualizar dados existentes.

---

### C4 — Campos do `LicenseCheckRequest` (backend)

**Problema:** Backend validava `deviceId`/`packageName`/`purchaseToken` (camelCase); Android enviava snake_case.

**Arquivos modificados:**

| Arquivo | Mudança |
|---|---|
| `backend/app/Http/Requests/Api/LicenseCheckRequest.php` | `'deviceId'` → `'device_id'`, `'packageName'` → `'package_name'`, `'purchaseToken'` → `'purchase_token'` |
| `backend/app/Http/Controllers/Api/V1/LicenseController.php` | `$request->input('purchaseToken')` → `$request->input('purchase_token')` |

**Bônus — simplificação do Android:**
`LicenseCheckRequest.kt` removeu `device_id` e `package_name` do body (dispositivo já identificado pelo Bearer token). Apenas `purchase_token` é enviado.

---

## Fase 2 — Correções Médias

---

### M1 — Schema do `ParserReport` (ambos os lados)

**Problema:** Backend exigia `success` como obrigatório; Android não enviava. DTOs tinham campos incompatíveis.

**Backend — `backend/app/Http/Requests/Api/ParserReportRequest.php`:**
- `'success'` alterado para `['nullable', 'boolean']`
- Campos legados adicionados e aceitos: `failed_pattern`, `event_type`, `timestamp`

**Android — `app/.../data/network/dto/LicenseDto.kt`:**
- `ParserFailureRequest` renomeado para `ParserReportRequest`
- Campos atualizados: `success` (Boolean), `rawTexts` (List<String>?), `errorMessage` (String?), `appVersion` (String?)

**Android — `SelectorRepositoryImpl.kt`:**
- `reportParserFailure()` agora usa `ParserReportRequest` com `success = false`

---

### M2 — Chaves de features da licença (backend)

**Problema:** Backend retornava `history_unlimited`/`export_enabled`; Android lia `unlimited_history`/`export`.

**Arquivo modificado:** `backend/app/Enums/LicensePlan.php`

| Antes (backend) | Depois (backend) | Android lê |
|---|---|---|
| `history_unlimited` | `unlimited_history` | `unlimited_history` ✅ |
| `export_enabled` | `export` | `export` ✅ |
| — | `cloud_backup` | `cloud_backup` ✅ |
| — | `multi_vehicle` | `multi_vehicle` ✅ |
| `advanced_rules` | removido | — |
| `max_rides` | removido | — |
| `history_days` | removido | — |

---

## Endpoint Novo — `POST /api/v1/subscription/validate`

**Implementado no Android:**

| Arquivo | Mudança |
|---|---|
| `app/.../data/network/dto/LicenseDto.kt` | `SubscriptionValidateRequest`, `SubscriptionValidateResponse` |
| `app/.../data/network/ApiService.kt` | `validateSubscription()` |
| `app/.../domain/repositories/LicenseRepository.kt` | `validateSubscription()` na interface |
| `app/.../data/repositories/LicenseRepositoryImpl.kt` | Implementação com tratamento de 422 |
| `app/.../licensing/LicenseValidator.kt` | `validateAndActivate()` — salva licença e atualiza StateFlow |
| `app/.../presentation/.../SubscriptionViewModel.kt` | `validateWithBackend()` → `LicenseValidator.validateAndActivate()` + fallback para `checkLicense` |

**Fluxo de restauração de compras:**
```
BillingManager.queryPurchases()
  → SubscriptionState.Subscribed(productId, purchaseToken)
  → LicenseValidator.validateAndActivate(productId, purchaseToken)
  → POST /api/v1/subscription/validate
  → 200: PRO ativado → UI atualizada
  → 422: fallback → POST /api/v1/license/check
```

---

## Baixo impacto — B1

### B1 — Campo `updated_at` no payload de seletores

**Arquivo modificado:** `backend/app/Models/SelectorVersion.php`
- `buildAndroidPayload()` agora inclui `'updated_at' => $this->published_at?->toIso8601String()`

---

## CLAUDE.md

Atualizado com:
- Seção "Android API Contract" com todos os contratos de request/response
- Fluxo de restauração de compras documentado
- Selector type format: lowercase obrigatório
- `DeviceRegistrar` documentado na arquitetura
- Tabela de endpoints backend atualizada
- Seção de prioridade de licenças

---

## Resumo de Arquivos

### Criados (4)
```
app/.../data/DeviceRegistrar.kt
app/.../data/network/dto/DeviceDto.kt
backend/database/migrations/2024_01_02_000001_lowercase_selector_types.php
```

### Modificados (14)
```
Android:
  app/.../data/network/ApiService.kt
  app/.../data/network/dto/LicenseDto.kt
  app/.../data/repositories/LicenseRepositoryImpl.kt
  app/.../data/repositories/SelectorRepositoryImpl.kt
  app/.../domain/repositories/LicenseRepository.kt
  app/.../licensing/LicenseValidator.kt
  app/.../presentation/screens/subscription/SubscriptionViewModel.kt

Backend:
  backend/app/Enums/FieldType.php
  backend/app/Enums/SelectorType.php
  backend/app/Enums/LicensePlan.php
  backend/app/Http/Requests/Api/LicenseCheckRequest.php
  backend/app/Http/Requests/Api/ParserReportRequest.php
  backend/app/Http/Controllers/Api/V1/LicenseController.php
  backend/app/Models/SelectorVersion.php
  backend/database/seeders/SelectorVersionSeeder.php

Documentação:
  CLAUDE.md
```
