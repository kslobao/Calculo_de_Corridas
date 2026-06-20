# Cálculo de Corridas — App Android

Aplicativo Android para **motoristas de aplicativo** (Uber Driver, 99 Motorista, inDrive, iFood Entregador) que monitora ofertas de corrida em tempo real via `AccessibilityService` e exibe um overlay flutuante com os dados extraídos — sem necessidade de root.

## Funcionalidades

- **Overlay flutuante** — exibe preço, distância e tempo estimado sobre qualquer tela
- **Detecção automática** por app — reconhece Uber, 99, inDrive e iFood simultaneamente
- **Seletores remotos** — padrões de extração atualizados pelo servidor sem nova versão do app
- **Histórico de corridas** — registro local com gráficos de desempenho (Vico Charts)
- **Filtros configuráveis** — preço mínimo, distância máxima, alertas sonoros/vibração
- **Plano PRO** — via Google Play Billing (assinatura mensal), licenças manuais (parceiros/beta)
- **Anúncios AdMob** — banner e intersticial para usuários do plano Free
- **WatchdogService** — reinicia serviços essenciais automaticamente após morte pelo sistema
- **Auto-inicialização** — retoma o monitoramento após reinicialização do dispositivo

## Requisitos

| Item | Mínimo |
|---|---|
| Android | 8.0 (API 26) |
| Permissão de Acessibilidade | Obrigatória |
| Permissão de Overlay | Obrigatória |
| Internet | Para validação de licença e seletores |

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitetura | MVVM + Clean Architecture + Hilt |
| Banco local | Room (SQLite) |
| Rede | Retrofit 2 + OkHttp |
| DI | Hilt |
| Assíncrono | Coroutines + Flow |
| Persistência | DataStore Preferences |
| Background | WorkManager |
| Firebase | Analytics + Crashlytics + Remote Config |
| Monetização | AdMob + Google Play Billing 6 |
| Gráficos | Vico Charts |
| Segurança | Jetpack Security Crypto |

## Fluxo de detecção de corrida

```
App de motorista abre oferta
         │
         ▼
AccessibilityService.onAccessibilityEvent()
         │
         ▼
RideDetectionEngine.processEvent()
         │  ─ percorre a árvore de Views
         │  ─ aplica seletores remotos (ACCESSIBILITY_ID / REGEX / CONTENT_DESC)
         │  ─ extraí preço, distância, tempo
         ▼
RideOfferRepository.saveOffer()   ──► Room (histórico local)
         │
         ▼
OverlayService recebe Flow<RideOffer>
         │
         ▼
Overlay flutuante renderizado via ComposeView
    ┌────┴────────────────────────┐
    │  💰 R$ 18,50               │
    │  📍 3,2 km  ⏱ 12 min      │
    │  [✓ Aceitar] [✗ Recusar]  │
    └────────────────────────────┘
```

## Estrutura de módulos

```
app/src/main/java/com/calculocorridas/
├── data/
│   ├── local/
│   │   ├── dao/               # RideDao, HistoryDao
│   │   └── database/          # AppDatabase (Room)
│   ├── remote/
│   │   ├── api/               # ApiService (Retrofit)
│   │   └── dto/               # AppSelectors, LicenseResponse, SelectorPattern
│   └── repository/            # Implementações dos repositórios
├── di/                        # Módulos Hilt (Network, Database, Repository)
├── domain/
│   ├── model/                 # RideOffer, License, AppConfig
│   ├── repository/            # Interfaces
│   └── usecase/               # ValidateLicenseUseCase, GetSelectorsUseCase...
├── presentation/
│   ├── MainActivity.kt
│   ├── screens/               # Home, History, Settings, Onboarding
│   └── components/            # Composables reutilizáveis
└── services/
    ├── accessibility/
    │   ├── RideAccessibilityService.kt   # Core do monitoramento
    │   └── RideDetectionEngine.kt        # Aplica seletores e extrai dados
    ├── overlay/
    │   └── OverlayService.kt             # FGS com ComposeView flutuante
    └── watchdog/
        ├── WatchdogService.kt            # Monitora e reinicia serviços
        └── BootReceiver.kt               # Auto-inicialização após boot
```

## Configuração do projeto

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17
- Android SDK 35

### Variáveis de build

Em [app/build.gradle.kts](app/build.gradle.kts):

```kotlin
// Debug — aponta para API de desenvolvimento
buildConfigField("String", "API_BASE_URL", "\"https://api-dev.calculocorridas.com/\"")

// Release — aponta para API de produção
buildConfigField("String", "API_BASE_URL", "\"https://api.calculocorridas.com/\"")
```

### google-services.json

Adicionar em `app/google-services.json` (obtido no Firebase Console). Necessário para Analytics, Crashlytics e Remote Config.

### Strings sensíveis

Em `app/src/main/res/values/strings.xml`:

```xml
<string name="admob_app_id">ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX</string>
```

> Nunca commitar AdMob IDs reais. Usar secrets no CI/CD.

## Seletores remotos

O app sincroniza padrões de detecção com o servidor via `GET /api/v1/selectors`. Isso permite atualizar a detecção quando os apps de motorista mudam sua interface, sem publicar uma nova versão.

```kotlin
// Estrutura recebida do servidor
data class AppSelectors(
    val version: Int,
    val uber: List<SelectorPattern>,
    val noventa_e_nove: List<SelectorPattern>,
    val indrive: List<SelectorPattern>,
    val ifood: List<SelectorPattern>
)

data class SelectorPattern(
    val field: String,       // "price" | "distance" | "time"
    val type: String,        // "ACCESSIBILITY_ID" | "REGEX" | "CONTENT_DESC"
    val value: String,       // o padrão em si
    val priority: Int
)
```

A versão atual é cacheada em DataStore. Se o servidor retornar `304 Not Modified`, o app usa o cache local.

## Licenças e monetização

### Plano Free
- Overlay com informações básicas
- Banner AdMob visível
- Intersticial a cada N corridas (configurável via Remote Config)
- Histórico limitado a 7 dias

### Plano PRO (assinatura Google Play)
- Overlay completo com todos os campos
- Sem anúncios
- Histórico ilimitado
- Gráficos avançados de desempenho
- Filtros e alertas configuráveis

### Verificação de licença (fluxo Android)

```kotlin
// POST /api/v1/license/check
{
    "device_token": "SHA256(ANDROID_ID + packageName)",
    "purchase_token": "token_do_google_play_ou_null"
}

// Resposta
{
    "plan": "pro",
    "source": "google",        // google | gift | partner | beta | admin
    "expires_at": "2026-12-31T23:59:59Z",
    "features": {
        "ads_free": true,
        "history_days": -1,    // -1 = ilimitado
        "max_rides": -1
    }
}
```

## Build para produção

```bash
# Gerar APK/AAB de release
./gradlew bundleRelease

# O AAB é gerado em:
# app/build/outputs/bundle/release/app-release.aab
```

Para assinar, configurar `keystore.properties` na raiz:

```properties
storeFile=../keystore/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

E referenciar no [app/build.gradle.kts](app/build.gradle.kts) via `signingConfigs`.

## Permissões necessárias (solicitadas ao usuário)

| Permissão | Quando | Por quê |
|---|---|---|
| Acessibilidade | Onboarding | Ler dados dos apps de motorista |
| Exibir sobre outros apps | Onboarding | Mostrar overlay flutuante |
| Notificações (Android 13+) | Primeira abertura | Alertas de corrida |

As demais permissões (`INTERNET`, `VIBRATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) são concedidas automaticamente pelo sistema.

## Repositório

```
Calculo_de_Corridas/
├── app/                    # Módulo Android
├── backend/                # API Laravel 11 + painel admin
│   └── README.md           # Documentação do backend
└── README.md               # Este arquivo
```
