# Cálculo de Corridas — Backend

API REST para o app Android **Cálculo de Corridas**, com painel administrativo completo. Gerencia licenças, assinaturas Google Play, seletores remotos de acessibilidade e configuração do app.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | PHP 8.3 |
| Framework | Laravel 11 |
| Banco de dados | PostgreSQL 16 |
| Cache / Filas | Redis 7 |
| Servidor web | Nginx 1.25 |
| Contêinerização | Docker Swarm |
| Admin panel | Bootstrap 5.3 |

## Arquitetura

```
Android App
    │
    ▼
Nginx (2 réplicas)
    │
    ▼
PHP-FPM / Laravel 11 (3 réplicas)
    ├── Redis (cache + filas)
    ├── PostgreSQL 16
    └── Queue Worker (2 réplicas) ──► Google Play Developer API
```

### Sistema de licenças — prioridade

```
Dispositivo faz POST /api/v1/license/check
    │
    ▼
1. Existe licença manual ativa? (gift / partner / beta / admin)
   └─► SIM → retorna PRO imediatamente (Google não é consultado)
    │
    ▼
2. purchaseToken enviado?
   └─► SIM → valida no Google Play → salva/atualiza Subscription + License
    │
    ▼
3. Existe licença Google válida no banco?
   └─► SIM → retorna PRO
    │
    ▼
4. Retorna FREE
```

**Assinaturas pertencem ao usuário**, não ao dispositivo. Um único `purchaseToken` cobre todos os dispositivos vinculados ao mesmo `user_id`.

## Endpoints da API

### Públicos (sem autenticação)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/health` | Saúde geral |
| `GET` | `/health/database` | Conectividade com PostgreSQL |
| `GET` | `/health/redis` | Conectividade com Redis |
| `POST` | `/api/v1/google/rtdn` | Webhook RTDN do Google Pub/Sub |

### Android — autenticação por Device Token

O token é `SHA256(ANDROID_ID + packageName)`, enviado como `Authorization: Bearer <token>`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/device/register` | Registra ou atualiza dispositivo |
| `GET` | `/api/v1/selectors` | Seletores remotos de acessibilidade (304 se atual) |
| `POST` | `/api/v1/license/check` | Verifica e retorna plano ativo |
| `POST` | `/api/v1/subscription/validate` | Valida purchase token no Google Play |
| `POST` | `/api/v1/parser/report` | Envia relatório de falha de parser |
| `GET` | `/api/v1/config` | Configuração pública do app |

### Admin panel

Acessível via `/admin`. Autenticação JWT armazenada em cookie HTTP-only.

| Seção | URL |
|---|---|
| Dashboard | `/admin/dashboard` |
| Dispositivos | `/admin/devices` |
| Usuários | `/admin/users` |
| Licenças | `/admin/licenses` |
| Assinaturas | `/admin/subscriptions` |
| Seletores | `/admin/selectors` |
| Remote Config | `/admin/config` |
| Relatórios | `/admin/reports` |
| Logs de auditoria | `/admin/logs` |

## Banco de dados

10 tabelas PostgreSQL 16:

```
users              — Usuários (UUID, SoftDeletes, multi-dispositivo)
admin_users        — Admins do painel (roles: admin / editor / viewer)
devices            — Dispositivos Android registrados
licenses           — Licenças manuais e Google Play (prioridade manual)
subscriptions      — Assinaturas Google Play com estado completo
selector_versions  — Versões dos seletores (apenas 1 ativa via UNIQUE parcial)
selectors          — Padrões por app × campo × tipo de seletor
parser_reports     — Relatórios de falha enviados pelo app
app_config         — Configuração remota com cache Redis
audit_logs         — Log de todas as ações administrativas
```

## Deploy com Docker Swarm

### Pré-requisitos

- Docker Engine ≥ 24 em modo Swarm (`docker swarm init`)
- Portainer CE ou BE (opcional, mas recomendado)
- Rede `traefik_public` externa criada: `docker network create --driver overlay traefik_public`

### 1. Configurar variáveis de ambiente

```bash
cp .env.example .env
# Editar .env com os valores de produção
```

Variáveis obrigatórias:

```env
APP_KEY=base64:...          # php artisan key:generate --show
JWT_SECRET=...              # php artisan jwt:secret --show
DB_DATABASE=calc_corridas
DB_USERNAME=laravel
DB_PASSWORD=senha_forte
REDIS_PASSWORD=senha_redis
APP_URL=https://api.seudominio.com
GOOGLE_PLAY_PACKAGE_NAME=com.calculocorridas
GOOGLE_PLAY_PRODUCT_ID=pro_monthly
GOOGLE_SERVICE_ACCOUNT_JSON={"type":"service_account",...}
RTDN_PUBSUB_TOKEN=token_secreto_pubsub
ADMIN_DEFAULT_EMAIL=admin@seudominio.com
ADMIN_DEFAULT_PASSWORD=senha_admin_forte
```

### 2. Criar diretórios de dados

```bash
mkdir -p /opt/calc-corridas/{postgres,redis,storage,logs}
```

### 3. Criar config do Nginx no Swarm

```bash
docker config create nginx_conf_v1 ./docker/nginx/default.conf
```

### 4. Construir e publicar a imagem

```bash
docker build -t registry.seudominio.com/calc-corridas-app:latest -f docker/php/Dockerfile .
docker push registry.seudominio.com/calc-corridas-app:latest
```

### 5. Deploy

```bash
# Via CLI
REGISTRY=registry.seudominio.com IMAGE_TAG=latest docker stack deploy -c stack.yml calc-corridas

# Via Portainer → Stacks → Add Stack → Upload stack.yml
```

### 6. Migrations e seed inicial

```bash
# Executa no contêiner app após o deploy
docker exec -it $(docker ps -qf name=calc-corridas_app) \
  php artisan migrate --seed --force
```

### Atualização rolling (zero downtime)

```bash
docker build -t registry.seudominio.com/calc-corridas-app:v1.1.0 .
docker push registry.seudominio.com/calc-corridas-app:v1.1.0
IMAGE_TAG=v1.1.0 docker stack deploy -c stack.yml calc-corridas
```

## Desenvolvimento local

Para desenvolvimento sem Swarm, use Docker Compose convencional:

```bash
docker compose -f docker-compose.dev.yml up -d
php artisan migrate --seed
php artisan serve
```

> O `stack.yml` é exclusivo para Swarm/Portainer. Não funciona com `docker-compose up`.

## Seletores remotos

Os seletores são padrões usados pelo `AccessibilityService` do Android para extrair dados das corridas. Gerenciados pelo painel em `/admin/selectors`.

- **ACCESSIBILITY_ID** — `resourceId` do elemento (`com.ubercab.driver:id/price`)
- **REGEX** — expressão regular aplicada ao texto (`R\$\s*([\d.,]+)`)
- **CONTENT_DESC** — texto do `contentDescription`
- **CLASS_NAME** — classe do `View`

Ao publicar uma nova versão, todos os dispositivos recebem os seletores atualizados automaticamente na próxima requisição `GET /api/v1/selectors` (resposta `200`). Versão já atual retorna `304 Not Modified`.

## Google Play Billing

### Fluxo RTDN (Real-time Developer Notifications)

```
Google Pub/Sub
    │ POST /api/v1/google/rtdn?token=RTDN_PUBSUB_TOKEN
    ▼
GoogleRtdnController → retorna 200 imediatamente
    │
    ▼ (assíncrono)
ProcessRtdnNotification (queue: rtdn)
    │
    ├── GooglePlayService → purchases.subscriptions.get
    └── SubscriptionService → atualiza Subscription + License no banco
```

### Configurar Pub/Sub no Google Cloud

1. Criar tópico Pub/Sub para RTDN nas configurações do Google Play Console
2. Criar assinatura Push apontando para `https://api.seudominio.com/api/v1/google/rtdn?token=SEU_TOKEN`
3. Definir `RTDN_PUBSUB_TOKEN` no `.env`

## Estrutura de diretórios relevante

```
backend/
├── app/
│   ├── Console/Commands/      # SyncExpiredSubscriptions, CleanOldParserReports
│   ├── Enums/                 # LicensePlan, LicenseSource, AppKey, FieldType...
│   ├── Http/
│   │   ├── Controllers/Api/   # Endpoints Android
│   │   ├── Controllers/Admin/ # Painel administrativo
│   │   ├── Middleware/        # DeviceAuth, AdminJwt, SecurityHeaders, Maintenance
│   │   └── Requests/          # Form requests com validação
│   ├── Jobs/                  # ProcessRtdnNotification, ValidateGoogleSubscription
│   ├── Models/                # 10 modelos Eloquent
│   ├── Repositories/          # Device, License, Selector repositories
│   └── Services/              # GooglePlay, License, Subscription, Rtdn...
├── database/migrations/       # 10 migrations
├── database/seeders/          # Admin, AppConfig, SelectorVersion seeders
├── docker/
│   ├── nginx/default.conf
│   └── php/Dockerfile
├── resources/views/admin/     # Blade views Bootstrap 5
├── routes/
│   ├── api.php                # Rotas Android
│   └── web.php                # Painel admin
└── stack.yml                  # Docker Swarm deploy
```

## Segurança

- Rate limiting por IP: 60 req/min (API geral), 10 req/min (registro), 120 req/min (RTDN)
- Device Token validado em cada requisição; bloqueio imediato refletido em tempo real
- JWT admin em cookie `HttpOnly; Secure; SameSite=Strict`; TTL 480 min
- Headers de segurança: `X-Frame-Options`, `X-Content-Type-Options`, HSTS (produção)
- Todas as ações do painel logadas em `audit_logs` com IP, valores antigos e novos
- Credenciais Google (`GOOGLE_SERVICE_ACCOUNT_JSON`) nunca expostas em logs
