# Izzi Webhook Alarm

Terminal Android (TV et téléphone) qui transforme un webhook en alarme sonore et visuelle.

```
Checkmate → POST /webhook/:deviceId → API VPS → WebSocket → appareil → son + écran
```

L'application Android est le client. L'API est dans [`server/`](server/) et le contrat est décrit dans [BACKEND_API.md](BACKEND_API.md).

Sur **Android TV**, l'interface reste en paysage avec des marges larges et la navigation télécommande. Sur **téléphone**, l'orientation est libre et la mise en page est compactée automatiquement.

## Compiler

Le JDK d'Android Studio et un SDK `compileSdk 37` sont requis. `local.properties` doit contenir `sdk.dir`. L'application cible Android 16 (`targetSdk 36`) et s'installe à partir d'Android 8.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

APK :

- `app/build/outputs/apk/debug/IzziWebhookAlarm-debug.apk`
- `app/build/outputs/apk/release/IzziWebhookAlarm-v<version>-<code>-release.apk` (ex. `IzziWebhookAlarm-v1.1.0-2-release.apk`)

L'écran d'accueil affiche `App v… (code)` pour vérifier la version installée.

Copiez la release versionnée dans `server/apk/` (alias stable : `IzziWebhookAlarm-release.apk`). `GET /download` sert la APK versionnée la plus récente du dossier. Les anciens noms `WebhookAlarm-TV-*` restent reconnus pour compatibilité.

L'APK release est signé avec le keystore debug pour une installation hors Play Store. Remplacez cette signature avant une diffusion plus large.

## Installer

**Android TV** — activez le débogage réseau, puis :

```bash
adb connect TV_IP:5555
adb install -r app/build/outputs/apk/debug/IzziWebhookAlarm-debug.apk
```

**Téléphone** — USB ou débogage sans fil :

```bash
adb install -r app/build/outputs/apk/debug/IzziWebhookAlarm-debug.apk
```

Aucun compte Google Play n'est nécessaire.

## Premier lancement

1. Renseignez l'URL du serveur (`https://alarm.example.com`) et, si le serveur a un `ALARM_TOKEN`, le même secret dans `Token (optional)`.
2. Le Device ID est une clé de 5 caractères (`K7M2P`), générée une fois pour l'appareil. Elle n'est pas saisie. Un ancien `device-001` est remplacé au prochain lancement : mettez à jour l'URL dans Checkmate.
3. `CONNECT` ouvre `wss://…/ws` et envoie `register`. L'état passe à `CONNECTED` seulement après la réponse `connected` du serveur.
4. `CONTINUE` ouvre l'écran principal.

Les réglages sont stockés dans DataStore Preferences. Il n'y a pas de base SQLite.

## Volume

Chaque niveau a son propre volume (info 40 %, warning 75 %, critical 100 % par défaut). Ce pourcentage s'applique au volume maximum, pas au niveau actuel du système.

Pendant une alarme, l'application monte les flux alarme et média à ce niveau, puis restaure le volume d'avant. Le baisser avant ou pendant l'alarme ne la rend pas plus faible. Sur une TV dont le volume est fixé par HDMI-CEC, Android refuse parfois ce changement et l'alarme reste au niveau du téléviseur.

## Alarmes

| Niveau   | Son                    | Volume | Lecture | Écran       | Acquittement |
| -------- | ---------------------- | ------ | ------- | ----------- | ------------ |
| info     | `res/raw/info.mp3`     | 40 %   | 1 fois  | bannière    | non          |
| warning  | `res/raw/warning.mp3`  | 75 %   | 3 fois  | overlay     | non          |
| critical | `res/raw/critical.mp3` | 100 %  | boucle  | plein écran | oui          |

`ACKNOWLEDGE` coupe le son critical et envoie `{ "type": "acknowledge", "alertId", "deviceId" }`. L'alerte reste affichée tant qu'un `alert_resolved` du même id n'arrive pas.

Après `connected`, le client envoie `{ "type": "ping" }` toutes les 15 secondes. Le serveur répond `{ "type": "pong" }`. Sans `pong` en 25 secondes, le client se reconnecte. Une coupure d'une session déjà ouverte est retentée en 300 ms. Les échecs d'ouverture restent en backoff (5 s, 10 s, 30 s, 60 s).

Les boutons `TEST INFO`, `TEST WARNING` et `TEST CRITICAL` passent par le même `AlertEngine` que les messages WebSocket. Ils fonctionnent hors ligne.

Les fichiers de `res/raw/` sont des tonalités de remplacement. Remplacez-les en gardant les mêmes noms.

## Démarrage au boot

`BootReceiver` écoute `BOOT_COMPLETED` et démarre un foreground service `specialUse`. Le service tient le WebSocket et un wake lock partiel pendant qu'il tourne. Quand un son est joué, le type du service passe à `specialUse|mediaPlayback`, puis revient à `specialUse`.

Au premier démarrage, l'appareil doit être configuré une fois. Les redémarrages suivants reconnectent seuls.

## Téléphone en arrière-plan

Sur **téléphone**, l'alarme ne repose plus sur l'écran d'accueil au premier plan. Trois mécanismes se cumulent :

1. **Notification d'alerte** (canal `Alerts`, priorité haute) : heads-up, titre, actions _Acknowledge_ / _Dismiss_ pour le critical, ouverture via tap.
2. **`AlertActivity`** : écran d'alarme dédié (`showWhenLocked`, `turnScreenOn`), lancé en **full-screen intent** pour warning/critical quand l'app n'est pas visible, et via `startActivity` depuis le service.
3. **Superposition système** (`SYSTEM_ALERT_WINDOW`) : si l'utilisateur autorise « Afficher par-dessus les autres apps », la même UI Compose que sur l'écran principal s'affiche par-dessus Chrome ou le launcher (warning/critical/ack/resolved ; info = notification seulement).

L'écran d'accueil affiche une carte **Background alerts** : notifications, overlay, full-screen intent (Android 14+) et batterie non restreinte. Il faut au minimum **notifications** + (**overlay** ou **full-screen intent**) pour un affichage fiable hors app.

Sur **Android TV**, le comportement reste centré sur `MainActivity` au premier plan ; la superposition n'est pas utilisée.

## Réseau

`http://` et `ws://` sont autorisés pour un serveur local. En production, utilisez `https://` et `wss://`.

Le champ `Token (optional)` est sur l'écran de configuration et dans les réglages. Vide, aucun en-tête n'est envoyé. Renseigné, le client envoie `Authorization: Bearer`. Le même secret va dans `ALARM_TOKEN` sur le serveur. Aucun secret n'est compilé dans l'application.

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

`GET /health` répond `{ "status": "ok" }`. Le client ouvre `GET /ws`, envoie `{ "type": "register", "deviceId" }` avec une clé de 5 caractères et reçoit `{ "type": "connected", "deviceId" }`. Checkmate appelle `POST /webhook/<deviceId>` ou `POST /webhook/all` pour notifier tous les appareils connectés (et ceux déjà connus du serveur).

Si l'appareil n'est pas connecté, ou si l'envoi échoue, l'alerte est gardée en mémoire (32 messages, 10 minutes) et répondue en `202` `{ "delivered": false, "queued": true, "type", "id" }`. Elle part au prochain `register`, après `connected`. Un redémarrage du process vide cette file.

`GET /download` renvoie l'APK (`application/vnd.android.package-archive`). Cette route est publique, même si `ALARM_TOKEN` est défini. Le fichier est cherché dans cet ordre :

1. `APK_PATH`, s'il est défini
2. `IzziWebhookAlarm-v*.apk` dans `server/apk/`
3. `server/apk/IzziWebhookAlarm-release.apk` (puis l'ancien `WebhookAlarm-TV-release.apk`)
4. l'APK Gradle release, puis debug

En Docker, l'image contient le dossier `server/apk/` à `/app/apk`.

```bash
cd server
npm test
```

### Docker Compose

Depuis `server/` :

```bash
docker compose up --build
```

L'API est sur `http://localhost:3000`. `HOST_PORT` change le port de l'hôte. L'APK vient de l'image, depuis `server/apk/`. `ALARM_TOKEN` reste vide tant qu'il n'est pas exporté.

### Coolify

Build pack Docker Compose. Répertoire de base : `server`. Fichier : `docker-compose.coolify.yml`.

Le service `api` déclare seulement `expose: 3000`. Il n'y a pas de `ports` ni de `PORT` : Coolify déduit le port interne de `expose`, puis renseigne `PORT`, le domaine et `SERVICE_URL`.

Variable créée par le Compose :

- `ALARM_TOKEN` : vide, ou le même secret que dans les réglages de l'app

L'APK est dans git (`server/apk/IzziWebhookAlarm-release.apk`) et copiée dans l'image au build. Un redeploy Coolify la met à jour.

Le client utilise `https://` et `wss://…/ws` sur ce domaine. Checkmate envoie `POST /webhook/<deviceId>`.
