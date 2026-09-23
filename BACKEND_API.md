# Backend API

API minimale à déployer plus tard sur un VPS. Pas de base de données, pas de Redis, pas de comptes. Une map en mémoire suffit pour le MVP :

```
deviceId → WebSocket
K7M2P    → socket A
B4NQ8    → socket B
```

`deviceId` est une clé de 5 caractères dans `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (sans `0`, `O`, `1`, `I`, `L`). L'application la génère une fois. `register` et `POST /webhook/:deviceId` refusent toute autre valeur.

Un token pourra être ajouté plus tard. Le MVP n'exige aucune authentification. Si la TV envoie `Authorization: Bearer <token>`, le backend pourra le vérifier sans changer le reste du protocole.

## GET /download

Réponse `200`, fichier APK (`application/vnd.android.package-archive`), en téléchargement. Aucun token n'est exigé.

Si aucun APK n'est disponible, `404` :

```json
{ "error": "APK not found" }
```

Le serveur lit `APK_PATH`, sinon `IzziWebhookAlarm-v*.apk` / `IzziWebhookAlarm-release.apk` dans `server/apk/` (puis les anciens noms `WebhookAlarm-TV-*`), sinon l'APK Gradle release puis debug.

## GET /health

Réponse `200` :

```json
{ "status": "ok" }
```

## WebSocket /ws

La TV ouvre `wss://alarm.example.com/ws`.

Dès l'ouverture, la TV envoie :

```json
{ "type": "register", "deviceId": "K7M2P" }
```

Une clé invalide est ignorée. Le serveur associe cette socket à `K7M2P` et répond :

```json
{ "type": "connected", "deviceId": "K7M2P" }
```

Les alertes en file pour cette clé sont ensuite envoyées, dans l'ordre. Si le même `deviceId` se reconnecte, l'ancienne socket est remplacée.

La TV envoie un heartbeat après `connected` :

```json
{ "type": "ping" }
```

Le serveur répond `{ "type": "pong" }`.

La TV peut ensuite envoyer un acquittement :

```json
{ "type": "acknowledge", "alertId": "evt_123", "deviceId": "K7M2P" }
```

`acknowledge` ne veut pas dire que l'incident est résolu. Le serveur peut le journaliser ou l'ignorer pour le MVP. Seul `alert_resolved` retire l'alerte de la TV.

## POST /webhook/all

Route réservée : diffuse la même alerte (ou résolution) vers **tous les appareils connus** — connectés en direct, ou déjà vus via une file ou une alerte précédente.

```
POST https://alarm.example.com/webhook/all
Content-Type: application/json
```

Même payload que `POST /webhook/:deviceId`. Réponse `202` :

```json
{
  "broadcast": true,
  "targets": 2,
  "delivered": 1,
  "queued": 1,
  "skipped": 0,
  "results": [
    {
      "deviceId": "K7M2P",
      "type": "alert",
      "id": "evt_123",
      "severity": "critical",
      "title": "Platform",
      "message": "Global incident",
      "timestamp": "2026-09-23T10:42:00Z"
    },
    {
      "deviceId": "B4NQ8",
      "delivered": false,
      "queued": true,
      "type": "alert",
      "id": "evt_456"
    }
  ]
}
```

`all` n'est pas une clé appareil : `POST /webhook/all` avec le segment dynamique `:deviceId` répond `400`.

## POST /webhook/:deviceId

Exemple :

```
POST https://alarm.example.com/webhook/K7M2P
Content-Type: application/json
```

Payload Checkmate, ou équivalent :

```json
{
  "severity": "critical",
  "text": "Production API is down",
  "monitor": { "name": "Production API" }
}
```

Le backend cherche la socket de `K7M2P`. Si elle est ouverte, il transforme le webhook et l'envoie :

```json
{
  "type": "alert",
  "id": "evt_123",
  "severity": "critical",
  "title": "Production API",
  "message": "Production API is down",
  "timestamp": "2026-09-23T10:42:00Z"
}
```

Règles de transformation :

- `title` = `monitor.name`, sinon un titre fourni, sinon le texte.
- `message` = `text` ou `message`.
- `severity` = `info`, `warning` ou `critical`.
- `id` = identifiant unique généré par le backend, ou l'id fourni par le webhook.
- `timestamp` = horodatage UTC ISO-8601.

Réponse webhook quand la TV est connectée : `202` avec l'événement envoyé.

Si la socket est absente ou si `send` échoue : `202`

```json
{ "delivered": false, "queued": true, "type": "alert", "id": "evt_123" }
```

La file garde au plus 32 messages par appareil, pendant 10 minutes. Le plus ancien est écarté. Un `alert` mis en file met à jour le dernier id, pour qu'une résolution hors ligne reste liée. Au `register`, la file est vidée après `connected`. Un redémarrage du process perd la file. Une clé invalide répond `400` `{ "error": "Invalid device id" }`.

## Résolution

Quand Checkmate signale le retour du service, le backend envoie à la même TV :

```json
{ "type": "alert_resolved", "id": "evt_123" }
```

L'`id` doit être celui de l'alerte d'origine. La TV ignore une résolution qui ne correspond à aucune alerte active.

## Sévérité

| severity | Comportement TV par défaut                                         |
| -------- | ------------------------------------------------------------------ |
| info     | bannière, son court                                                |
| warning  | overlay, son répété                                                |
| critical | plein écran, son en boucle jusqu'à ACKNOWLEDGE ou `alert_resolved` |

## Hors ligne

Une alerte arrivée pendant une coupure reste en file et part au prochain `register`. La file est en mémoire : elle ne survit pas à un redémarrage du process.
