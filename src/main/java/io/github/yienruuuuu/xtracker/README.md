# XTracker API Notes

This module uses the observed XTracker posts API behavior below. Keep this file
updated when live calls show that the upstream shape has changed.

## Person posts endpoint

```powershell
curl.exe -k "https://xtracker.polymarket.com/api/users/elonmusk/posts?platform=X&startDate=2026-05-15T16%3A00%3A00Z&endDate=2026-05-22T16%3A00%3A00Z"
```

- Method: `GET`
- Endpoint: `https://xtracker.polymarket.com/api/users/{handle}/posts`
- Required query: `platform`, usually `X`
- Optional query: `startDate`, `endDate`
- Use UTC absolute instants for time windows, for example `2026-05-15T16:00:00Z`.
- Do not send `timezone` when `startDate` or `endDate` are absolute instants.
- Local PowerShell `Invoke-RestMethod` may hit certificate or 403 behavior; `curl.exe -k` has been the practical probe command in local diagnostics.

## Response shape

Observed response:

```json
{
  "success": true,
  "data": [
    {
      "id": "cmpgmjwxi0006ic04lt27s8ct",
      "userId": "c4e2a911-36ec-4453-8a39-1edb5e6b2969",
      "platformId": "2057730745821483416",
      "content": "RT @tetsuoai: https://t.co/RFO8ZtukZc",
      "createdAt": "2026-05-22T07:50:15.000Z",
      "importedAt": "2026-05-22T07:55:08.023Z",
      "metrics": null
    }
  ]
}
```

Important field meanings:

- `id` is the XTracker internal post id.
- `platformId` is the source-platform post id. For `platform=X`, this is the X status id and must be used for normalized identity and public X URLs.
- `createdAt` is the source post time.
- `importedAt` is when XTracker imported the post.

## Known Elon boundary

The earliest observed Elon data currently available from XTracker is:

```text
2025-10-31T00:17:35Z
```

Use UTC half-open windows `[startAt, endAt)` for backfill and market queries.
