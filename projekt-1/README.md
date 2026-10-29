## Architektura

```
projekt-1/
├── backend/            Spring Boot 3 (Java 17, Maven) + PostgreSQL        → port 8080
├── frontend/           React (Vite) serwowany przez Node/Express          → port 3000 (dev: 5173)
├── docker-compose.yml  lokalne uruchomienie: db + backend + frontend
└── terraform/          (kolejny etap) infrastruktura AWS
```

Moduły są od siebie niezależne – frontend rozmawia z backendem wyłącznie przez HTTP (REST + CORS),
każdy ma własny `Dockerfile` i może być hostowany osobno (docelowo: dwa serwisy AWS Fargate).

### Backend

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/api/messages` | wszystkie wiadomości (parametr `?slow=true` sztucznie wydłuża odpowiedź o 3 s – na potrzeby testów CloudWatch) |
| GET | `/api/messages/new?after=<ISO-8601>` | wiadomości nowsze niż podany znacznik czasu (frontend odpytuje co 3 s) |
| POST | `/api/messages` | nowa wiadomość: `{ "username", "content", "fileId"? }` |
| POST | `/api/messages/{id}/like` | polubienie wiadomości |
| POST | `/api/files` | przesłanie pliku multimedialnego (`multipart/form-data`, pole `file`, max 10 MB) |
| GET | `/api/files/{id}` | pobranie pliku |
| GET | `/actuator/health` | health check (dla load balancera) |

Pliki: metadane trafiają do bazy (tabela `files`), zawartość do `FileStorage`.
Lokalnie jest to katalog na dysku (`LocalFileStorage`); w chmurze zostanie podmieniony na implementację S3
(`FILE_STORAGE_TYPE=s3`) bez zmian w kontrolerach.

Cała konfiguracja przez zmienne środowiskowe (wartości domyślne pasują do `docker-compose.yml`):

| Zmienna | Domyślnie | Opis |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/chat` | adres bazy |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `chat` / `chat` | dane logowania do bazy |
| `FILE_STORAGE_TYPE` | `local` | `local` lub (docelowo) `s3` |
| `FILE_STORAGE_DIR` | `./data/files` | katalog na pliki przy `local` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | adresy frontendu (po przecinku) |
| `SERVER_PORT` | `8080` | port |

### Frontend

Jednoekranowy czat: nick, lista wiadomości (obrazki wyświetlane inline), wysyłanie tekstu i/lub pliku, polubienia.
W trybie produkcyjnym `server.js` (Express) serwuje zbudowane pliki i generuje `/config.js`
ze zmiennej `API_BASE_URL` – dzięki temu ten sam obraz Dockera działa lokalnie i w chmurze.

| Zmienna | Domyślnie | Opis |
|---|---|---|
| `API_BASE_URL` | `http://localhost:8080` | adres backendu **widziany z przeglądarki** |
| `PORT` | `3000` | port serwera Node |

W trybie deweloperskim (`npm run dev`) adres backendu bierze się z `public/config.js`.

## Uruchomienie lokalne

Wszystkie polecenia wykonuj w katalogu `projekt-1/` (z katalogu repozytorium: `cd projekt-1`).

### Wariant A – wszystko w Dockerze (zalecany)

Wymagania: Docker Desktop.

```bash
docker compose up --build
```

- frontend: http://localhost:3000
- backend: http://localhost:8080/api/messages (health: http://localhost:8080/actuator/health)
- baza: `localhost:5432`, baza/użytkownik/hasło: `chat`

Zatrzymanie: `Ctrl+C`, a potem `docker compose down` (dodaj `-v`, żeby skasować też dane bazy i pliki).

### Wariant B – backend i frontend z IDE, baza w Dockerze

Wymagania: JDK 17+, Maven (wbudowany w IntelliJ IDEA) lub `winget install Apache.Maven`, Node.js 22, Docker Desktop.

1. Baza danych:
   ```bash
   docker compose up db
   ```
2. Backend (w drugim terminalu lub z IntelliJ: otwórz `backend/pom.xml` jako projekt i uruchom `ChatApplication`):
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Pliki trafiają do `backend/data/files/`.
3. Frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Aplikacja: http://localhost:5173 (hot reload).

### Szybki test API bez frontendu

```bash
curl -X POST http://localhost:8080/api/messages -H "Content-Type: application/json" -d "{\"username\":\"ja\",\"content\":\"hej\"}"
curl http://localhost:8080/api/messages
curl -F "file=@zdjecie.png" http://localhost:8080/api/files
```

## Budowanie obrazów ręcznie

```bash
docker build -t chat-backend ./backend
docker build -t chat-frontend ./frontend

docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/chat chat-backend
docker run -p 3000:3000 -e API_BASE_URL=http://localhost:8080 chat-frontend
```

## AWS – kroki 4 i 5 (AWS CLI + Terraform)

### Poświadczenia Learner Lab (powtarzane po każdym *Start Lab*)

1. AWS Academy → Learner Lab → **Start Lab** → po zmianie kółka na zielone **AWS Details** → przy *AWS CLI* kliknij **Show**.
2. Skopiuj cały blok `[default] ... aws_session_token=...` do pliku `C:\Users\<login>\.aws\credentials` (nadpisz poprzedni).
3. Sprawdź: `aws sts get-caller-identity` – ma zwrócić numer konta.

Klucze wygasają z końcem sesji labu – przy błędach typu `ExpiredToken` / `InvalidClientTokenId` powtórz kroki 1–2.

### Terraform (`projekt-1/terraform/`)

```bash
cd terraform
copy terraform.tfvars.example terraform.tfvars   # uzupełnij db_password i alert_email
terraform init          # pobiera providery (raz)
terraform validate
terraform plan          # podgląd ~40 zasobów
terraform apply         # tworzy infrastrukturę (ok. 10 min, najdłużej RDS)
.\push-images.ps1       # buduje obrazy i wypycha do ECR (repozytoria muszą już istnieć)
terraform output        # adresy frontendu i backendu
```

Po pierwszym `apply` AWS wysyła na `alert_email` mail *AWS Notification – Subscription Confirmation* – kliknij link, inaczej alarmy nie dotrą.

Serwisy ECS startują od razu po `apply`, ale dopóki obrazy nie są w ECR, zadania będą się restartować – to normalne; po `push-images.ps1` same wstaną w ciągu 1–2 min.

**Po pracy: `terraform destroy`** – RDS, dwa ALB i Fargate liczą się godzinowo i zjadają budżet Learner Lab.

### Struktura katalogu `terraform/`

Terraform czyta **wszystkie pliki `.tf` z jednego katalogu** i traktuje je jak jeden plik – nazwy nie mają
znaczenia dla działania, podkatalogi nie są wczytywane. Podział na pliki służy tylko czytelności, dlatego
nazwy mają numeryczny prefiks, który dzieli je na dwie grupy i narzuca kolejność w Eksploratorze/GitHubie:

- **`0x-`** – konfiguracja samego Terraforma (nie tworzy zasobów w AWS),
- **`1x-`** – zasoby AWS, ponumerowane w kolejności zależności: każdy plik korzysta tylko z tego,
  co zdefiniowano w plikach o niższym numerze (np. `16-ecs.tf` odwołuje się do ALB z `15-`, bazy z `13-`,
  bucketa z `12-`, obrazów z `11-` i security groups z `10-`).

| Plik | Rola | Pkt |
|---|---|---|
| `00-providers.tf` | wymagane wtyczki (`aws`, `archive`), region, domyślne tagi | – |
| `01-variables.tf` | parametry wejściowe (`var.*`): region, prefiks nazw, hasło bazy, e-mail… | – |
| `02-data.tf` | odczyt zasobów, które już istnieją w Learner Lab: `LabRole`, domyślny VPC i podsieci, ID konta | – |
| `03-outputs.tf` | wartości wypisywane po `apply` (adresy URL, ID Cognito, nazwa bucketa) | – |
| `10-security-groups.tf` | zapory: internet → ALB (80), ALB → kontenery (8080/3000), kontenery → baza (5432) | 9 |
| `11-ecr.tf` | 2 repozytoria obrazów Dockera | 9 |
| `12-s3.tf` | bucket na pliki multimedialne + blokada publicznego dostępu | 7 |
| `13-rds.tf` | PostgreSQL `db.t3.micro` + grupa podsieci | 8 |
| `14-cognito.tf` | pula użytkowników + klient aplikacji | 6 |
| `15-alb.tf` | 2 load balancery (backend / frontend) z listenerami i target groups | 2, 9 |
| `16-ecs.tf` | klaster, 2 task definitions (tu zbiegają się wszystkie zmienne środowiskowe), 2 serwisy Fargate, log groups | 9, 10 |
| `17-cloudwatch.tf` | SNS + subskrypcja e-mail, alarm czasu odpowiedzi, alarm 5xx, dashboard | 10 |
| `18-lambda.tf` | Lambda wyzwalana przez upload do S3 (kod w `lambda/handler.py`) | 11 |
| `lambda/handler.py` | kod funkcji Lambda (Python) – dane dla `18-lambda.tf` | 11 |
| `push-images.ps1` | skrypt PowerShell: build + push obrazów do ECR (poza Terraformem) | 9 |
| `terraform.tfvars.example` | wzór pliku ze zmiennymi; kopiuje się go jako `terraform.tfvars` | – |

Pliki generowane lokalnie, poza repozytorium: `.terraform/` (wtyczki), `terraform.tfvars` (hasło, e-mail),
`terraform.tfstate` (rejestr utworzonych zasobów – nie kasować), `media_processor.zip` (spakowana Lambda).
W repozytorium jest natomiast `.terraform.lock.hcl` – przypina wersje wtyczek.

Wszystko powstaje w domyślnym VPC i z gotową rolą `LabRole`, bo Learner Lab nie pozwala tworzyć ról IAM.

## Mapa na wymagania projektu

| Pkt | Wymaganie | Status |
|---|---|---|
| 1 | aplikacja: ≥2 GET, ≥2 POST + POST/GET plików | ✔ `GET /api/messages`, `GET /api/messages/new`, `POST /api/messages`, `POST /api/messages/{id}/like`, `POST/GET /api/files` |
| 2 | osobne moduły backend / frontend, hostowane niezależnie | ✔ `backend/`, `frontend/` |
| 3 | Docker dla każdego modułu | ✔ `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` |
| 4 | AWS CLI | ✔ instrukcja powyżej |
| 5, 12 | Terraform – cała infrastruktura | ✔ `terraform/` |
| 6 | Cognito | user pool w Terraformie; integracja w aplikacji – do zrobienia |
| 7 | S3 | bucket w Terraformie; implementacja `FileStorage` dla S3 – do zrobienia |
| 8 | RDS (PostgreSQL) | ✔ aplikacja gotowa; instancja RDS w Terraformie |
| 9 | Fargate | ✔ `terraform/ecs.tf`, `alb.tf` |
| 10 | CloudWatch | ✔ `?slow=true` w aplikacji; alarmy, SNS i dashboard w `terraform/cloudwatch.tf` |
| 11 | Lambda | ✔ `terraform/lambda.tf` (metadane + tag po uploadzie do S3) |
| 13 | ręczny odpowiednik infrastruktury | do zrobienia |
| 14 | raport | do zrobienia |
