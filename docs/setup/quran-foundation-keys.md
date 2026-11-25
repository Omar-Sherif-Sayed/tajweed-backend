# The Quran Foundation's keys — how to keep them on your computer (2026-11-22)

The tafsir module (F5, F26) syncs the tafsirs from the Quran Foundation. It needs the app's **client id** and **client
secret** from their Developer Console. They must never be written in a file of any repository (all five are public),
never in a chat, never in an e-mail. The backend reads them only from three environment variables:

| Variable | Value |
| --- | --- |
| `QF_CLIENT_ID` | the client id of the environment you use |
| `QF_CLIENT_SECRET` | the secret of the same environment |
| `QF_ENV` | `prelive` first (it has surahs 1 – 2 only), `production` after their approval |

## On this Windows computer — once

1. Open **PowerShell** yourself (Start → PowerShell). Not the agent's chat.
2. Run these three lines, one at a time. The first two **ask** for the value: paste it at the prompt and press Enter.
   The value is not written in PowerShell's history (only the command is).

   ```powershell
   $v = Read-Host 'Quran Foundation client id (pre-live)'; [Environment]::SetEnvironmentVariable('QF_CLIENT_ID', $v, 'User'); Remove-Variable v
   ```

   ```powershell
   $v = Read-Host 'Quran Foundation client secret (pre-live)'; [Environment]::SetEnvironmentVariable('QF_CLIENT_SECRET', $v, 'User'); Remove-Variable v
   ```

   ```powershell
   [Environment]::SetEnvironmentVariable('QF_ENV', 'prelive', 'User')
   ```

3. **Close and reopen the Claude app** (and any terminal): programs read the environment when they start.
4. Tell the agent «المفاتيح جاهزة». The agent then runs `./gradlew liveTest`, which syncs the tafsirs from pre-live into
   a temporary test database and prints **only counts** (how many rows, how many tafsirs cover 1:1) — never the id, the
   secret, a token or a text.

⚠ Honest limit: a user environment variable can be read by any program you run under your account — the agent
included. The agent's rule is never to print, log, copy or commit them; the backend never logs them either (its
errors name the address called, without the query, and never a header).

## The first live check (2026-11-26)

The keys work. Pre-live offers only the tafsirs 159, 169, 170, 381, 804 and 926 — **none of the seven Arabic tafsirs
we chose**, so on pre-live the sync records «none of the chosen tafsirs is offered here» and writes nothing. To check
the mechanism there, the live test syncs tafsir 169 into its throwaway database:

```powershell
$env:QF_LIVE_TAFSIRS = '169'; .\gradlew.bat liveTest
```

Result: 293 rows (al-Fatihah and al-Baqarah, one per ayah), 1:1 covered. The chosen tafsirs come with the Quran Foundation's
**production** environment — their data, not the release of our app. Not needed now (the author, 2026-11-26); when the
tafsir screens are built or before release: look in their console whether the production client already has the two
scopes the sync uses — `content.tafsirs.read` (the tafsirs) and `content.sync.read` (Content Sync, their only permitted
way to keep a copy). Pre-live has both: its token was granted with them. Ask only for what is missing, then set the
production keys and `QF_ENV=production`.

## Production (2026-11-26)

The author set the production keys (`QF_ENV=production`). The token was granted with both scopes, so nothing had to be
requested. `./gradlew liveTest`: the seven chosen tafsirs synced — 42,623 rows, each of the seven with a text for 1:1.
The pre-live keys were replaced by the production ones; they are still in the author's console if pre-live is needed.

## Later

- **Production**: when the Quran Foundation approves the app for production, set the production id and secret the
  same way and `QF_ENV` to `production`.
- **The server**: on the production server the same three variables are set in the service's environment (or its
  secret store) — `docs/deployment/` when hosting is chosen (`RT-01`).
- **If a secret leaks**: rotate it at once in their Developer Console (their terms also ask that a breach be reported
  to developers@quran.com within 24 hours), then set the new value as above.
- **To remove them** from this computer:

  ```powershell
  'QF_CLIENT_ID','QF_CLIENT_SECRET','QF_ENV' | ForEach-Object { [Environment]::SetEnvironmentVariable($_, $null, 'User') }
  ```
