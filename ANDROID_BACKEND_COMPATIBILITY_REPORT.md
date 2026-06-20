# Relatório de Compatibilidade Android ↔ Backend

**Data:** 2026-06-20 (re-auditoria após correções)  
**Versão anterior:** auditoria inicial com 4 críticos, 2 médios, 3 baixos  
**Versão atual:** todas as issues resolvidas

---

## Resultado Final

| Severidade | Antes | Depois |
|---|---|---|
| 🔴 CRÍTICO | 4 | 0 ✅ |
| 🟡 MÉDIO | 2 | 0 ✅ |
| 🟢 BAIXO | 3 | 0 ✅ |

---

## Matriz de Compatibilidade — Estado Atual

| Endpoint | Autenticação | Request Body | Response | Status |
|---|---|---|---|---|
| `POST /api/v1/device/register` | ✅ sem auth | ✅ `DeviceRegisterRequest` | ✅ `DeviceRegisterResponse` | ✅ |
| `GET /api/v1/selectors?version=N` | ✅ Bearer | ✅ query param | ✅ `price_patterns` snake_case, tipos lowercase | ✅ |
| `POST /api/v1/license/check` | ✅ Bearer | ✅ `purchase_token` snake_case | ✅ features com keys corretas | ✅ |
| `POST /api/v1/subscription/validate` | ✅ Bearer | ✅ `product_id` + `purchase_token` | ✅ `SubscriptionValidateResponse` | ✅ |
| `POST /api/v1/parser/report` | ✅ Bearer | ✅ `success` nullable, campos unificados | ✅ | ✅ |
| `GET /api/v1/config` | ✅ Bearer | — | ✅ | ✅ |
| `POST /api/v1/google/rtdn` | ✅ token query | ✅ | ✅ | ✅ |
| `GET /health` | — | — | ✅ | ✅ |

---

## Verificação por Issue

### 🔴 C1 — Registro de dispositivo

**Antes:** Android não tinha `POST /api/v1/device/register`. Todos os endpoints protegidos retornavam 401.  
**Depois:** `DeviceRegistrar.ensureRegistered()` chamado automaticamente antes de `checkRemote()` e `getRemote()`. Registro lazy, idempotente, persistido em DataStore. ✅

**Verificação:**
```kotlin
// LicenseRepositoryImpl.checkRemote()
deviceRegistrar.ensureRegistered()  // ← chama register se ainda não feito
val response = api.checkLicense(...)

// SelectorRepositoryImpl.getRemote()
deviceRegistrar.ensureRegistered()  // ← mesma proteção
val response = api.getSelectors(...)
```

---

### 🔴 C2 — Chaves de campo dos seletores

**Antes:** Backend enviava `pricePatterns` (camelCase); Android desserializava `price_patterns` (snake_case) → listas vazias.  
**Depois:** `FieldType.androidKey()` retorna `price_patterns`, `distance_patterns`, etc. ✅

**Verificação:**
```php
// FieldType.php
self::Price => 'price_patterns',   // ✅ bate com @SerializedName("price_patterns")
```
```kotlin
// AppSelectorsDto.kt
@SerializedName("price_patterns") val pricePatterns: List<SelectorPatternDto>  // ✅
```

---

### 🔴 C3 — Tipo de seletor uppercase vs lowercase

**Antes:** Backend enviava `ACCESSIBILITY_ID`; `SelectorType.fromKey()` buscava `accessibility_id` → todos viravam REGEX.  
**Depois:** `SelectorType` enum usa `accessibility_id`, `regex`, `content_desc`, `class_name`. Migration converte dados existentes. ✅

**Verificação:**
```php
// SelectorType.php
case AccessibilityId = 'accessibility_id';  // ✅
```
```kotlin
// Selector.kt
ACCESSIBILITY_ID("accessibility_id"),       // ✅ fromKey() encontra corretamente
```
```sql
-- migration
UPDATE selectors SET selector_type = LOWER(selector_type);  -- ✅
```

---

### 🔴 C4 — Campos do `LicenseCheckRequest`

**Antes:** Backend validava `purchaseToken` (camelCase); Android enviava `purchase_token` → sempre null.  
**Depois:** Regras snake_case; controller usa `purchase_token`. Android simplificado (remove `device_id` redundante). ✅

**Verificação:**
```php
// LicenseCheckRequest.php
'purchase_token' => ['nullable', 'string', 'max:500'],  // ✅

// LicenseController.php
$request->input('purchase_token')  // ✅
```
```kotlin
// LicenseDto.kt
data class LicenseCheckRequest(
    @SerializedName("purchase_token") val purchaseToken: String?  // ✅
)
```

---

### 🟡 M1 — Schema do `ParserReport`

**Antes:** `success` obrigatório → 422 em todos os reports do Android.  
**Depois:** `success` nullable; campos legados aceitos; Android atualizado com `ParserReportRequest`. ✅

**Verificação:**
```php
// ParserReportRequest.php
'success' => ['nullable', 'boolean'],          // ✅ não mais obrigatório
'failed_pattern' => ['nullable', 'string', ...], // ✅ aceita campos legados
```
```kotlin
// LicenseDto.kt
data class ParserReportRequest(
    val appKey: String,
    val selectorVersion: Int,
    val success: Boolean,   // ✅ enviado corretamente
    ...
)
```

---

### 🟡 M2 — Chaves de features da licença

**Antes:** `history_unlimited`/`export_enabled` → Android lia `unlimited_history`/`export` → sempre false.  
**Depois:** Backend retorna exatamente os campos que o Android lê. ✅

**Verificação:**
```php
// LicensePlan.php — Pro features
'ads_free'          => true,   // ✅ Android: @SerializedName("ads_free")
'unlimited_history' => true,   // ✅ Android: @SerializedName("unlimited_history")
'cloud_backup'      => true,   // ✅ Android: @SerializedName("cloud_backup")
'export'            => true,   // ✅ Android: @SerializedName("export")
'multi_vehicle'     => true,   // ✅ Android: @SerializedName("multi_vehicle")
```

---

### 🟢 B1 — Campo `updated_at` no payload de seletores

**Antes:** `SelectorConfig.updatedAt` ficava null.  
**Depois:** `buildAndroidPayload()` inclui `updated_at`. ✅

---

## Novo Endpoint — `POST /api/v1/subscription/validate`

Implementado no Android. Fluxo completo de compra nova e restauração:

```
Google Play confirma compra
        │
        ▼
SubscriptionState.Subscribed(productId, purchaseToken)
        │
        ▼
LicenseValidator.validateAndActivate(productId, purchaseToken)
        │
        ▼
POST /api/v1/subscription/validate  →  GooglePlayService.isSubscriptionActive()
        │
        ├── 200: License salva → StateFlow<License> atualizado → UI PRO ✅
        └── 422: fallback → POST /api/v1/license/check
```

**Casos cobertos:**
- ✅ Nova compra
- ✅ Restauração de compras (troca de dispositivo)
- ✅ Usuário logado em múltiplos dispositivos
- ✅ Fallback para `license/check` se `subscription/validate` falhar

---

## Contratos de API Finais

### `POST /api/v1/device/register`
```json
// Request
{ "device_token": "a1b2c3...64chars", "package_name": "com.calculocorridas", "app_version": "1.0.0" }

// Response 201
{ "registered": true, "device_id": "uuid", "plan": "free" }
```

### `GET /api/v1/selectors?version=2`
```json
// Response 200
{
  "version": 2,
  "updated_at": "2026-06-20T00:00:00Z",
  "apps": {
    "uber": {
      "price_patterns": [
        { "type": "accessibility_id", "value": "com.ubercab.driver:id/trip_fare", "priority": 100 },
        { "type": "regex",            "value": "R\\$\\s*([\\d.,]+)",               "priority": 50  }
      ],
      "distance_patterns": [...],
      "time_patterns":     [...],
      "origin_patterns":   [...],
      "destination_patterns": [...],
      "category_patterns": [...]
    },
    "99": { ... }, "indrive": { ... }, "ifood": { ... }
  }
}
// Response 304 — versão atual, sem body
```

### `POST /api/v1/license/check`
```json
// Request
{ "purchase_token": "tok_google_xyz_or_null" }

// Response 200
{
  "active": true,
  "plan": "pro",
  "source": "google",
  "expires_at": "2026-12-31T23:59:59Z",
  "features": {
    "ads_free": true, "unlimited_history": true,
    "cloud_backup": true, "export": true, "multi_vehicle": true
  }
}
```

### `POST /api/v1/subscription/validate`
```json
// Request
{ "product_id": "pro_monthly", "purchase_token": "tok_google_xyz" }

// Response 200
{
  "valid": true, "plan": "pro", "source": "google",
  "expires_at": "2026-12-31T23:59:59Z",
  "features": { "ads_free": true, "unlimited_history": true, ... }
}

// Response 422
{ "valid": false, "reason": "subscription_invalid_or_expired" }
```

### `POST /api/v1/parser/report`
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
// Response 201
{ "received": true }
```

---

## Ações pós-deploy obrigatórias

```bash
# 1. Aplicar migration de lowercase (CRÍTICO para dados existentes)
php artisan migrate

# 2. Se já houver dados no seeder rodados, re-seed ou migração via SQL:
# UPDATE selectors SET selector_type = LOWER(selector_type);
```

---

**Conclusão:** Todos os 9 problemas identificados na auditoria inicial foram corrigidos. O contrato de API entre Android e backend está completamente alinhado. Nenhuma incompatibilidade restante.
