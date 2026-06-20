# Cálculo de Corridas — Backend

API REST para o app Android **Cálculo de Corridas**, com painel administrativo completo. Gerencia licenças, assinaturas Google Play, seletores remotos de acessibilidade e configuração do app.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | PHP 8.3 |
| Framework | Laravel 11 |
| Banco de dados | PostgreSQL 16 |
| Cache / Filas | Redis 7 |
| Servidor web | Nginx 1.25 (imagem customizada) |
| Contêinerização | Docker Swarm |
| Admin panel | Bootstrap 5.3 + DataTables |

## Arquitetura

```
Internet
    │
    ▼
Traefik (80/443 — TLS via Let's Encrypt)
    │
    ▼
Nginx customizado (2 réplicas — sem portas públicas)
    │  docker/nginx/Dockerfile + default.conf embutido
    ▼
PHP-FPM / Laravel 11 (3 réplicas)
    ├── Redis 7 (cache + filas)
    ├── PostgreSQL 16
    └── Queue Worker (2 réplicas) ──► Google Play Developer API
```

> O Nginx não expõe portas ao host. O Traefik é o único edge router e faz SSL termination.

## Duas imagens Docker

| Imagem | Dockerfile | Conteúdo |
|---|---|---|
| `calc-corridas-app` | `docker/php/Dockerfile` | PHP-FPM + Laravel |
| `calc-corridas-nginx` | `docker/nginx/Dockerfile` | Nginx + config embutida |

## Endpoints da API

### Públicos (sem autenticação)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/up` | Health check do Laravel |
| `GET` | `/nginx-health` | Health check do Nginx (retorna `ok`) |
| `POST` | `/api/v1/google/rtdn` | Webhook RTDN do Google Pub/Sub |

### Android — autenticação por Device Token

Token = `SHA256(ANDROID_ID + packageName)`, enviado como `Authorization: Bearer <token>`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/device/register` | Registra dispositivo (deve ser a primeira chamada) |
| `GET` | `/api/v1/selectors?version=N` | Seletores remotos (304 se versão atual) |
| `POST` | `/api/v1/license/check` | Verifica e retorna plano ativo |
| `POST` | `/api/v1/subscription/validate` | Valida purchase token no Google Play |
| `POST` | `/api/v1/parser/report` | Envia relatório de falha de parser |
| `GET` | `/api/v1/config` | Configuração pública do app |

### Admin panel

Acessível via `/admin`. Autenticação JWT em cookie HTTP-only `admin_token`.

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
licenses           — Licenças manuais e Google Play
subscriptions      — Assinaturas Google Play com estado completo
selector_versions  — Versões dos seletores (1 ativa via UNIQUE parcial)
selectors          — Padrões por app × campo × tipo (valores sempre lowercase)
parser_reports     — Relatórios de falha enviados pelo app
app_config         — Configuração remota com cache Redis
audit_logs         — Log de todas as ações administrativas
```

### Sistema de licenças — prioridade

```
POST /api/v1/license/check
  1. Licença manual ativa? (gift/partner/beta/admin) → PRO imediatamente
  2. purchase_token enviado? → valida no Google Play → salva → PRO
  3. Licença Google válida no banco? → PRO
  4. Retorna FREE
```

Assinaturas pertencem ao **usuário**, não ao dispositivo. Um `purchaseToken` cobre todos os dispositivos do mesmo `user_id`.

## Deploy com Docker Swarm

### Pré-requisitos no servidor

```bash
# Swarm já inicializado
docker swarm init

# Rede externa do Traefik
docker network create --driver overlay traefik_public

# Diretórios de dados persistentes
mkdir -p /opt/calc-corridas/{postgres,redis,storage,logs}
```

### 1. Gerar segredos

```bash
# APP_KEY
docker run --rm registry.hostdatec.com.br/calc-corridas-app:v1.0.0 \
  php artisan key:generate --show

# JWT_SECRET
openssl rand -hex 32

# REDIS_PASSWORD e DB_PASSWORD
openssl rand -hex 16

# RTDN_PUBSUB_TOKEN
openssl rand -hex 24
```

### 2. Build e push das duas imagens

```bash
cd backend/

# Imagem PHP-FPM
docker build -t registry.hostdatec.com.br/calc-corridas-app:v1.0.0 \
  -f docker/php/Dockerfile .
docker push registry.hostdatec.com.br/calc-corridas-app:v1.0.0

# Imagem Nginx (config já embutida)
docker build -t registry.hostdatec.com.br/calc-corridas-nginx:v1.0.0 \
  -f docker/nginx/Dockerfile docker/nginx/
docker push registry.hostdatec.com.br/calc-corridas-nginx:v1.0.0
```

### 3. Deploy via Portainer

1. **Stacks → Add Stack**
2. Cole o conteúdo do `stack.yml`
3. Em **Environment variables** → **Load variables from .env file** → selecione seu `.env`
4. **Deploy the stack**

> Certifique-se de que `REGISTRY`, `IMAGE_TAG`, `DOMAIN` e todas as variáveis obrigatórias estão preenchidas.

### 4. Migrations e seed inicial

```bash
# Após o deploy, identificar o container
docker ps --format "{{.ID}} {{.Names}}" | grep app

# Rodar migrations + seed
docker exec -it $(docker ps -qf "name=calculo_de_corridas_app" | head -1) \
  php artisan migrate --seed --force
```

Se a migration já foi rodada parcialmente e o seed falhou:

```bash
# Corrigir constraint manualmente (só na primeira instalação problemática)
docker exec -it $(docker ps -qf "name=calculo_de_corridas_postgres" | head -1) \
  psql -U corridas_user -d calculo_corridas -c "
ALTER TABLE selectors DROP CONSTRAINT IF EXISTS chk_selectors_selector_type;
ALTER TABLE selectors ADD CONSTRAINT chk_selectors_selector_type
  CHECK (selector_type IN ('accessibility_id','regex','content_desc','class_name'));
"

# Re-seed só dos seletores
docker exec -it $(docker ps -qf "name=calculo_de_corridas_app" | head -1) \
  php artisan db:seed --class=SelectorVersionSeeder --force
```

### 5. Atualização rolling (zero downtime)

```bash
# Build das novas imagens
docker build -t registry.hostdatec.com.br/calc-corridas-app:v1.1.0 \
  -f docker/php/Dockerfile .
docker push registry.hostdatec.com.br/calc-corridas-app:v1.1.0

# Só se o Nginx config mudou:
docker build -t registry.hostdatec.com.br/calc-corridas-nginx:v1.1.0 \
  -f docker/nginx/Dockerfile docker/nginx/
docker push registry.hostdatec.com.br/calc-corridas-nginx:v1.1.0

# Rolling update (start-first: nova réplica sobe antes de derrubar a antiga)
docker service update \
  --image registry.hostdatec.com.br/calc-corridas-app:v1.1.0 \
  app_calculo_de_corridas_app

docker service update \
  --image registry.hostdatec.com.br/calc-corridas-nginx:v1.1.0 \
  app_calculo_de_corridas_nginx
```

## Monitoramento

```bash
# Status dos serviços
docker stack services app_calculo_de_corridas

# Logs em tempo real (Laravel loga para stderr)
docker service logs -f app_calculo_de_corridas_app
docker service logs -f app_calculo_de_corridas_queue-worker

# Health checks
curl https://api.calculocorridas.com/up
curl https://api.calculocorridas.com/nginx-health
```

> **Nota:** `LOG_CHANNEL=stderr` — os logs do Laravel aparecem via `docker logs` / `docker service logs`, não em arquivo. Não existe `storage/logs/laravel.log` em produção.

## Seletores remotos

Padrões usados pelo `AccessibilityService` Android para extrair dados das corridas. Gerenciados em `/admin/selectors`.

| Tipo | Valor no banco | Descrição |
|---|---|---|
| `accessibility_id` | lowercase | `resourceId` do elemento |
| `regex` | lowercase | Expressão regular aplicada ao texto |
| `content_desc` | lowercase | `contentDescription` do elemento |
| `class_name` | lowercase | Classe do `View` |

> ⚠️ **Sempre lowercase** — tanto no banco quanto no wire format. A constraint PostgreSQL e o enum Android esperam lowercase. Nunca inserir uppercase.

Ao publicar nova versão, todos os dispositivos recebem os seletores atualizados na próxima chamada `GET /api/v1/selectors`. Versão já atual retorna `304 Not Modified`.

## Google Play Billing — RTDN

```
Google Pub/Sub
    │ POST /api/v1/google/rtdn?token=RTDN_PUBSUB_TOKEN
    ▼
GoogleRtdnController → retorna 200 imediatamente
    │ (assíncrono via fila rtdn)
    ▼
ProcessRtdnNotification → GooglePlayService → atualiza Subscription + License
```

**Configurar Pub/Sub:**
1. Google Play Console → Monetização → Real-time notifications → criar tópico
2. Criar assinatura Push para `https://api.calculocorridas.com/api/v1/google/rtdn?token=SEU_TOKEN`
3. Definir `RTDN_PUBSUB_TOKEN` no `.env`

## Segurança

- Device Token validado em cada requisição via `DeviceAuthMiddleware`
- JWT admin em cookie `HttpOnly; Secure; SameSite=Strict`; TTL 480 min
- Rate limiting por IP: 60 req/min API, 30 req/min admin
- Headers: `X-Frame-Options`, `X-Content-Type-Options`, `HSTS`
- Todas as ações do painel logadas em `audit_logs` com IP e diff old/new
- `GOOGLE_SERVICE_ACCOUNT_JSON` nunca exposto em logs
