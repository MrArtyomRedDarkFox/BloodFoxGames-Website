# BloodFoxGames frontend overlay

This folder adds a responsive Steam-inspired storefront UI to the existing coursework project.
It uses the existing game-store API and adds only two backend endpoints needed by the UI:

- `GET /api/v1/games` — game catalog.
- `POST /api/v1/users/login` — verifies a username and BCrypt password hash.

The UI is served by Nginx on port `3000`; Nginx forwards `/api/*` to `game-store-service` in the Docker Compose network.

## Apply the overlay

Recommended: extract this ZIP into a folder, open a terminal there, and run:

```bash
chmod +x install-overlay.sh
./install-overlay.sh
```

The script backs up the files it will replace into `~/BloodFoxGames-ui-backup-<timestamp>`, copies the frontend/backend changes into `~/BloodFoxGames---game-store-and-game-library`, and creates `.env` from `.env.example` only if `.env` does not already exist. It does not push anything to GitHub.

If your repository is stored elsewhere, pass its path, e.g. `./install-overlay.sh /path/to/BloodFoxGames---game-store-and-game-library`.

Manual alternative: copy the contents of this overlay into the repository root, preserving folders and allowing these files to be replaced:

- `.gitignore` (new; if you already have one, merge the `.env` and `*.bak` entries)
- `README.md`
- `docker-compose.yml`
- `frontend/` (new)
- `game-store-service/src/main/java/com/example/gamestore/controller/GameController.java` (new)
- `game-store-service/src/main/java/com/example/gamestore/dto/LoginDto.java` (new)
- `game-store-service/src/main/java/com/example/gamestore/controller/UserController.java`
- `game-store-service/src/main/java/com/example/gamestore/repository/UserRepository.java`
- `game-store-service/src/main/java/com/example/gamestore/service/UserService.java`
- `.env.example` (new)

Before overwriting files, make a backup if you have made any extra local changes. The versions here retain the known endpoints for registration, library, purchase, BCrypt, and notifications.

## Run

From the repository root:

```bash
For local development, configure POSTGRES_PASSWORD in your untracked .env file. Never commit .env.
cp .env.example .env
For an existing db_data volume, POSTGRES_PASSWORD must match the PostgreSQL role password originally configured when the database was initialized. Changing .env alone does not change an existing database password.
# If you change it, also update the PostgreSQL role password or use a fresh DB volume.
sudo docker compose config
sudo docker compose up --build -d
sudo docker compose ps
```

Open the storefront in the Ubuntu browser: <http://localhost:3000>.

API checks:

```bash
curl -i http://localhost:8080/api/v1/games
curl -i http://localhost:8082/health
```

## User flow

1. Open **Создать аккаунт** and register with a test username, email and password.
2. Sign in using the same username and password.
3. Open **Магазин**, select a game, and click **Купить игру**.
4. Open **Библиотека** and check that the game is listed.
5. `sudo docker compose logs --tail=50 notifications-service` should show the purchase notification.

## Notes

- This is a coursework demo, not a production storefront. The browser stores only the displayed user ID/name/email in `localStorage`; it does not store the password. API authorization is still based on user IDs in URLs, so add server-side sessions or JWT and ownership checks before any real deployment.
- Existing test accounts created before BCrypt was introduced may have plaintext passwords in the database and will not be able to log in through the new BCrypt login endpoint. Register a new test account to verify the flow.
- The database volume is intentionally preserved. Do not run `docker compose down -v` unless you explicitly intend to delete local database data.
