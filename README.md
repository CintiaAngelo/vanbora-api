# VanBora API — Backend (Spring Boot)

API REST do aplicativo VanBora. Autenticação via **JWT**, persistência em **MySQL**,
arquitetura em camadas por feature, princípios **SOLID** e **Clean Code**.

## Stack

- Java 21 · Spring Boot 3.3
- Spring Web · Spring Data JPA · Spring Security (JWT) · Bean Validation
- MySQL 8 · springdoc-openapi (Swagger UI)

## Pré-requisitos

- **JDK 21** (já instalado nesta máquina em `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`).
- **MySQL 8** rodando em `localhost:3306` (instale o MySQL Community + Workbench).
- Não é necessário instalar o Maven: use o **Maven Wrapper** (`mvnw`) incluído.

## Configuração do banco

**Passo 1 — crie o banco e o usuário da aplicação.** Abra `database/00_setup_user.sql`,
troque `TROQUE_ESTA_SENHA` por uma senha sua e execute no MySQL Workbench (conectado
como `root`). Ele cria o banco `vanbora` e o usuário dedicado `vanbora` — assim você
não usa o root da máquina. **Não salve a senha escolhida no arquivo:** ele é versionado.

> Perdeu a senha do root? `database/reset_root.ps1` reinicia o MySQL com um
> `--init-file` temporário e redefine root + `vanbora` com as senhas que você digitar.

**Passo 2 — configure as variáveis de ambiente.** Copie `.env.example` para `.env`
(ignorado pelo git) e preencha. A API lê esse arquivo sozinha na inicialização —
funciona igual no IntelliJ e no `mvnw`, sem configurar nada na IDE. Variáveis de
ambiente de verdade têm precedência sobre ele, que é como produção sobrescreve tudo.

**`DB_PASSWORD` não tem valor padrão**: senha de banco não fica versionada, então a
API não sobe sem ela — e avisa exatamente isso se faltar.

| Variável             | Obrigatória | Observação                                        |
|----------------------|-------------|---------------------------------------------------|
| `DB_URL`             | não         | default: `jdbc:mysql://localhost:3306/vanbora?...` |
| `DB_USERNAME`        | não         | default: `vanbora`                                 |
| `DB_PASSWORD`        | **sim**     | a senha criada no passo 1                          |
| `VANBORA_JWT_SECRET` | em produção | ≥ 32 caracteres aleatórios                         |
| `SERVER_PORT`        | não         | default: `8080`                                    |

A lista completa (rate limiting, CORS, Auth0, taxas) está em `.env.example`.

> O schema é criado automaticamente (`ddl-auto=update`). Se preferir criar à mão no
> Workbench, rode `../database/vanbora_schema.sql`. Dados de demonstração são populados
> na primeira execução (banco vazio).

## Como rodar

```powershell
# a partir da pasta backend/, com o .env já preenchido
.\mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa:
`http://localhost:8080/swagger-ui.html`.

## Logins de demonstração (senha: `123456`)

- **Responsável:** `mariana@vanbora.com` — 3 filhos (um com transporte ativo, um com
  contrato aguardando assinatura e um sem transportador), endereço geocodificado,
  cartão salvo, histórico de mensalidades e 2 conversas.
- **Transportador:** `roberto@vanbora.com` — 6 alunos, rota do dia com ETA, painel
  financeiro com 6 meses de histórico, 6 conversas, 5 avisos e 2 solicitações pendentes.
- **Escola:** `secretaria@objetivo.com.br` (painel de gestão do Colégio Objetivo).

Há ainda 4 transportadores concorrentes (`fernanda@`, `carlos@`, `patricia@`,
`anderson@`, `simone@vanbora.com`) e 5 responsáveis (`juliana@`, `marcos@`, `renata@`,
`thiago@`, `camila@vanbora.com`), todos com a mesma senha — usados para a tela de busca
e para as conversas.

### Fotos dos dados de demonstração

O seed aponta as fotos (perfis, dependentes, ajudantes, veículos e imagens no chat) para
`/uploads/seed/…`. Como `uploads/` é mídia de runtime e não vai para o git, baixe-as uma
vez antes de semear o banco:

```powershell
powershell -ExecutionPolicy Bypass -File .\database\download_seed_media.ps1
```

### Repopular do zero

O seed só roda com o banco vazio. Para regerar (dados relativos a "hoje", então nunca
ficam desatualizados), apague as tabelas e suba a API de novo:

```sql
-- no MySQL Workbench, conectado ao schema `vanbora`
DROP DATABASE vanbora; CREATE DATABASE vanbora CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

## Arquitetura

```
com.vanbora.api
├── config/            Segurança, CORS, OpenAPI, seeding de dados
├── security/          JWT (filtro, serviço), UserDetails, usuário atual
├── shared/            Base de entidades, enums, exceções e handler global
└── modules/           Pacotes por feature (cada um com domain/repository/service/dto + controller)
    ├── auth/          Cadastro e login (emite JWT)
    ├── user/          Identidade (User)
    ├── guardian/      Perfil, dependentes, painel e pagamentos do responsável
    ├── transporter/   Busca, perfil público/próprio e painel do transportador
    ├── enrollment/    Matrículas (alunos) e presença
    ├── hire/          Solicitações de contratação
    ├── payment/       Mensalidades
    ├── notice/        Avisos em massa
    ├── finance/       Resumo financeiro
    ├── route/         Rota do dia
    └── chat/          Conversas e mensagens
```

## Segurança

**Sessão.** A API é *stateless* e **não usa cookies**: o app manda o JWT no header
`Authorization: Bearer`. O token carrega um `jti` (identificador único), e
`POST /api/account/logout` coloca esse `jti` numa denylist (`revoked_tokens`, também
em memória para não consultar o banco a cada requisição). Sem isso, uma cópia do
token continuaria valendo até expirar mesmo depois de o usuário sair da conta.
Só o token daquele aparelho é revogado — as outras sessões seguem ativas.

Token ausente, expirado ou revogado responde **401**; usuário autenticado sem
permissão para o recurso responde **403**. São situações diferentes e o app trata
cada uma de um jeito (401 = sessão acabou, volta para o login).

**Rate limiting** (`RateLimitFilter`), por IP, em três faixas configuráveis:

| Faixa   | Rotas                                | Default    |
|---------|--------------------------------------|------------|
| `AUTH`  | `/api/auth/**`                       | 20 req/min |
| `WRITE` | POST/PUT/PATCH/DELETE nas demais     | 120 req/min|
| `READ`  | o restante                           | 300 req/min|

Estouro devolve **429** com `Retry-After`. Isso complementa o `LoginAttemptService`,
que bloqueia *uma conta* após 5 senhas erradas: o bloqueio por conta não impede
*password spraying* (uma senha comum testada em milhares de e-mails), o limite por
IP impede. Atrás de proxy reverso, ligue `VANBORA_RATE_LIMIT_TRUST_PROXY=true` para
usar o `X-Forwarded-For` — sem proxy, deixe desligado (o cabeçalho seria forjável).

**Checagens na inicialização** (`SecurityStartupChecks`). Em produção
(`VANBORA_ENV=production`) a API **não sobe** com: JWT secret padrão ou curto,
`DB_PASSWORD` vazio, CORS ainda apontando para localhost/rede local, seed de
demonstração ligado, ou `AUTH0_DOMAIN` sem `AUTH0_AUDIENCE`. Em desenvolvimento os
mesmos itens viram avisos no log.

**Login social (Auth0 — Google e Facebook).** `POST /api/auth/social` recebe o
`id_token` que o app obteve do Auth0. A API não confia no conteúdo: verifica a
assinatura RS256 contra o JWKS do tenant, exige o algoritmo RS256, o emissor e a
audiência (client id do app), e só aceita e-mail confirmado pelo provedor. Duas
respostas possíveis:

- e-mail já cadastrado → devolve a sessão pronta (`registered: true`);
- primeiro acesso → devolve nome/e-mail e um **ticket de cadastro** de curta duração
  (`registered: false`). O app segue para a escolha de perfil e o cadastro normal
  (CPF, endereço, CNH…) enviando o ticket no lugar da senha.

O ticket é assinado com uma chave *derivada* do JWT secret, não com ele: assim um
ticket nunca passa por um token de sessão no filtro de autenticação. Só valores
públicos ficam configurados (domínio e client id) — o client secret do Auth0 não é
usado nem pela API nem pelo app, porque o fluxo é Authorization Code + PKCE.

**Decisões de design**
- *SRP / camadas:* cada controller delega a um service; os services dependem de
  interfaces de repositório (Spring Data) — não de implementações concretas.
- *DIP:* casos de uso expostos por interfaces (`AuthService`, `GuardianService`, …)
  e injetados por construtor.
- *DTOs + records:* a borda HTTP nunca expõe entidades JPA diretamente.
- *Tratamento de erros centralizado* no `GlobalExceptionHandler` (respostas `ApiError`).
- *Stateless:* sem sessão de servidor; o token JWT carrega identidade e papel.

## Principais endpoints

| Método | Rota | Perfil |
|--------|------|--------|
| POST | `/api/auth/register/guardian` | público |
| POST | `/api/auth/register/transporter` | público |
| POST | `/api/auth/login` | público |
| POST | `/api/auth/social` | público (login com Google/Facebook) |
| GET  | `/api/auth/me` | autenticado |
| POST | `/api/account/logout` | autenticado (revoga o token) |
| GET  | `/api/guardians/me/dashboard` | responsável |
| GET  | `/api/guardians/me/payments` | responsável |
| GET/POST | `/api/guardians/me/dependents` | responsável |
| GET  | `/api/transporters?school=&neighborhood=&sort=` | autenticado |
| GET  | `/api/transporters/{id}` | autenticado |
| POST | `/api/hire-requests` | responsável |
| GET  | `/api/transporters/me/dashboard` | transportador |
| GET  | `/api/transporters/me/students` | transportador |
| GET/POST | `/api/transporters/me/notices` | transportador |
| GET  | `/api/transporters/me/finance` | transportador |
| GET  | `/api/transporters/me/route` | transportador |
| POST | `/api/transporters/me/route/optimize` | transportador |
| POST | `/api/transporters/me/location` | transportador |
| GET  | `/api/guardians/me/tracking` | responsável |
| POST | `/api/hire-requests/{id}/accept` \| `/reject` | transportador |
| GET  | `/api/conversations` · `/api/conversations/{id}/messages` | autenticado |
```
#   v a n b o r a - a p i  
 