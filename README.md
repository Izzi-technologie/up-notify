# Webhook Alarm TV

Terminal Android TV qui transforme un webhook en alarme sonore et visuelle.

```
Checkmate → POST /webhook/:deviceId → API VPS → WebSocket → Android TV → son + écran
```

L'application Android TV est le client. L'API est dans [`server/`](server/) et le contrat est décrit dans [BACKEND_API.md](BACKEND_API.md).

## Compiler

Le JDK d'Android Studio et un SDK `compileSdk 37` sont requis. `local.properties` doit contenir `sdk.dir`. L'application cible Android 16 (`targetSdk 36`) et s'installe à partir d'Android 8.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

APK :

- `app/build/outputs/apk/debug/WebhookAlarm-TV-debug.apk`
- `app/build/outputs/apk/release/WebhookAlarm-TV-release.apk`

Copiez la release dans `server/apk/WebhookAlarm-TV-release.apk` et committez-la. L'image Docker sert ce fichier sur `GET /download`.

L'APK release est signé avec le keystore debug pour une installation hors Play Store. Remplacez cette signature avant une diffusion plus large.

## Installer sur une Android TV

Activez le débogage réseau sur la TV, puis :

```bash
adb connect TV_IP:5555
adb install -r app/build/outputs/apk/debug/WebhookAlarm-TV-debug.apk
```

Aucun compte Google Play n'est nécessaire.

## Premier lancement

1. Renseignez l'URL du serveur (`https://alarm.example.com`), le Device ID (`tv-001`) et, si le serveur a un `ALARM_TOKEN`, le même secret dans `Token (optional)`.
2. `CONNECT` ouvre `wss://…/ws` et envoie `register`.
3. Quand l'état passe à `CONNECTED`, `CONTINUE` ouvre l'écran principal.

Les réglages sont stockés dans DataStore Preferences. Il n'y a pas de base SQLite.

## Volume

Chaque niveau a son propre volume (info 40 %, warning 75 %, critical 100 % par défaut). Ce pourcentage s'applique au volume maximum, pas au niveau actuel de la TV.

Pendant une alarme, l'application monte les flux alarme et média à ce niveau, puis restaure le volume d'avant. Le baisser avant ou pendant l'alarme ne la rend pas plus faible. Sur une TV dont le volume est fixé par HDMI-CEC, Android refuse ce changement et l'alarme reste au niveau du téléviseur.

## Alarmes

| Niveau   | Son                    | Volume | Lecture | Écran       | Acquittement |
| -------- | ---------------------- | ------ | ------- | ----------- | ------------ |
| info     | `res/raw/info.mp3`     | 40 %   | 1 fois  | bannière    | non          |
| warning  | `res/raw/warning.mp3`  | 75 %   | 3 fois  | overlay     | non          |
| critical | `res/raw/critical.mp3` | 100 %  | boucle  | plein écran | oui          |

`ACKNOWLEDGE` coupe le son critical et envoie `{ "type": "acknowledge", "alertId", "deviceId" }`. L'alerte reste affichée tant qu'un `alert_resolved` du même id n'arrive pas.

Les boutons `TEST INFO`, `TEST WARNING` et `TEST CRITICAL` passent par le même `AlertEngine` que les messages WebSocket. Ils fonctionnent hors ligne.

Les fichiers de `res/raw/` sont des tonalités de remplacement. Remplacez-les en gardant les mêmes noms.

## Démarrage au boot

`BootReceiver` écoute `BOOT_COMPLETED` et démarre un foreground service `specialUse`. Le service tient le WebSocket et un wake lock partiel pendant qu'il tourne. Quand un son est joué, le type du service passe à `specialUse|mediaPlayback`, puis revient à `specialUse`.

Une notification avec `fullScreenIntent` tente d'ouvrir l'écran au boot et lors d'une alerte critical. Certains OEM bloquent l'ouverture d'une activité depuis l'arrière-plan. Dans ce cas, le socket et le son continuent : ouvrez l'application depuis la notification ou le lanceur.

Au premier démarrage, la TV doit être configurée une fois. Les redémarrages suivants reconnectent seuls.

## Réseau

`http://` et `ws://` sont autorisés pour un serveur local. En production, utilisez `https://` et `wss://`.

Le champ `Token (optional)` est sur l'écran de configuration et dans les réglages. Vide, aucun en-tête n'est envoyé. Renseigné, la TV envoie `Authorization: Bearer`. Le même secret va dans `ALARM_TOKEN` sur le serveur. Aucun secret n'est compilé dans l'application.

## Backend

L'API tient dans `server/`. Une map en mémoire associe chaque `deviceId` à sa WebSocket. Il n'y a pas de base, pas de Redis et pas de comptes.

```bash
cd server
npm install
npm run dev
```

Le process écoute `PORT` (défaut `3000`).

`ALARM_TOKEN` est optionnel et lu uniquement depuis l'environnement. S'il est vide, le webhook et le WebSocket n'exigent pas d'authentification. S'il est défini, les deux exigent `Authorization: Bearer`.

```bash
PORT=3000 ALARM_TOKEN=secret npm run dev
```

`GET /health` répond `{ "status": "ok" }`. La TV ouvre `GET /ws`, envoie `{ "type": "register", "deviceId" }` et reçoit `{ "type": "connected", "deviceId" }`. Checkmate appelle `POST /webhook/<deviceId>`.

`GET /download` renvoie l'APK (`application/vnd.android.package-archive`). Cette route est publique, même si `ALARM_TOKEN` est défini. Le fichier est cherché dans cet ordre :

1. `APK_PATH`, s'il est défini
2. `server/apk/WebhookAlarm-TV-release.apk`
3. l'APK Gradle release, puis debug

En Docker, l'image contient `server/apk/WebhookAlarm-TV-release.apk` à `/app/apk`.

```bash
cd server
npm test
```

### Docker Compose

Depuis `server/` :

```bash
docker compose up --build
```

L'API est sur `http://localhost:3000`. `HOST_PORT` change le port de l'hôte. L'APK vient de l'image, depuis `server/apk/WebhookAlarm-TV-release.apk`. `ALARM_TOKEN` reste vide tant qu'il n'est pas exporté.

### Coolify

Build pack Docker Compose. Répertoire de base : `server`. Fichier : `docker-compose.coolify.yml`.

Le service `api` déclare seulement `expose: 3000`. Il n'y a pas de `ports` ni de `PORT` : Coolify déduit le port interne de `expose`, puis renseigne `PORT`, le domaine et `SERVICE_URL`.

Variable créée par le Compose :

- `ALARM_TOKEN` : vide, ou le même secret que dans les réglages de la TV

L'APK est dans git (`server/apk/WebhookAlarm-TV-release.apk`) et copiée dans l'image au build. Un redeploy Coolify la met à jour.

La TV utilise `https://` et `wss://…/ws` sur ce domaine. Checkmate envoie `POST /webhook/<deviceId>`.
