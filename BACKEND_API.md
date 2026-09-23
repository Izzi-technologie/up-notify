# Backend API

API minimale à déployer plus tard sur un VPS. Pas de base de données, pas de Redis, pas de comptes. Une map en mémoire suffit pour le MVP :

```
deviceId → WebSocket
tv-001   → socket A
tv-002   → socket B
```

Un token pourra être ajouté plus tard. Le MVP n'exige aucune authentification. Si la TV envoie `Authorization: Bearer <token>`, le backend pourra le vérifier sans changer le reste du protocole.

## GET /download

Réponse `200`, fichier APK (`application/vnd.android.package-archive`), en téléchargement. Aucun token n'est exigé.

Si aucun APK n'est disponible, `404` :

```json
{ "error": "APK not found" }
```

Le serveur lit `APK_PATH`, sinon `server/apk/WebhookAlarm-TV-release.apk`, sinon l'APK Gradle release puis debug.

## GET /health

Réponse `200` :

```json
{ "status": "ok" }
```

## WebSocket /ws

La TV ouvre `wss://alarm.example.com/ws`.

Dès l'ouverture, la TV envoie :

```json
{ "type": "register", "deviceId": "tv-001" }
```

Le serveur associe cette socket à `tv-001` et répond :

```json
{ "type": "connected", "deviceId": "tv-001" }
```

Si le même `deviceId` se reconnecte, l'ancienne socket est remplacée.

La TV peut ensuite envoyer un acquittement :

```json
{ "type": "acknowledge", "alertId": "evt_123", "deviceId": "tv-001" }
```

`acknowledge` ne veut pas dire que l'incident est résolu. Le serveur peut le journaliser ou l'ignorer pour le MVP. Seul `alert_resolved` retire l'alerte de la TV.

## POST /webhook/:deviceId

Exemple :

```
POST https://alarm.example.com/webhook/tv-001
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

Le backend cherche la socket de `tv-001`. Si elle est absente, répondre `404`. Sinon, transformer le webhook et l'envoyer sur la socket :

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

Si la TV n'est pas connectée, le webhook ne peut pas être délivré. Le MVP ne met pas les événements en file. Checkmate renverra un webhook quand le service changera à nouveau d'état.
