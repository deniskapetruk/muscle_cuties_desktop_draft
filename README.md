
# muscle_cuties

Two projects:
- muscle_cuties_back — Java server (chat + workout planner)
- muscle_cuties_front — Java Swing client

Shared port in `port.txt` at repository root (default 8000).

## IntelliJ IDEA
1. File → Open → select the `muscle_cuties` folder.
2. Right‑click `muscle_cuties_back/src` → Mark Directory As → Sources Root.
3. Right‑click `muscle_cuties_front/src` → Mark Directory As → Sources Root.
4. Ensure a valid JDK (11+ recommended) in Project Structure.
5. Run configs:
   - Backend: Application → Main class: `MuscleServer` (Working directory can be project root or `muscle_cuties_back`).
   - Frontend: Application → Main class: `app.Main` (Working directory can be project root or `muscle_cuties_front`).

## Accounts
- Client: `alice` / `alice`
- Client: `bella` / `bella`
- Trainer: `coach` / `coach`

## Usage
- Start the server run config.
- Start the client run config.
- Clients see a Workout tab (auto-refresh table, semicolon parsing) and Chat tab.
- Trainer sees a left list of available client sessions (only assigned), can Attach and chat.
- Phase is hidden in UI and rotates weekly on server from account creation.
