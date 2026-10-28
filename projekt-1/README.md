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

## Mapa na wymagania projektu

| Pkt | Wymaganie | Status |
|---|---|---|
| 1 | aplikacja: ≥2 GET, ≥2 POST + POST/GET plików | ✔ `GET /api/messages`, `GET /api/messages/new`, `POST /api/messages`, `POST /api/messages/{id}/like`, `POST/GET /api/files` |
| 2 | osobne moduły backend / frontend, hostowane niezależnie | ✔ `backend/`, `frontend/` |
| 3 | Docker dla każdego modułu | ✔ `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` |
| 4 | AWS CLI | do zrobienia |
| 5, 12 | Terraform – cała infrastruktura | do zrobienia (`terraform/`) |
| 6 | Cognito | do zrobienia |
| 7 | S3 | do zrobienia (`FileStorage` → implementacja S3) |
| 8 | RDS (PostgreSQL) | ✔ aplikacja gotowa; instancja RDS w Terraformie |
| 9 | Fargate | do zrobienia |
| 10 | CloudWatch (parametr spowalniający) | ✔ `?slow=true` w aplikacji; metryki/alarmy w Terraformie |
| 11 | Lambda | do zrobienia |
| 13 | ręczny odpowiednik infrastruktury | do zrobienia |
| 14 | raport | do zrobienia |
