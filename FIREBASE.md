# Firebase Setup

The website stays on GitHub Pages. Firebase project `ballbuddy-97095` supplies
anonymous player identities, private challenge progress, callable functions,
and standings. Players receive a unique automatic name, editable on results;
there is no Google sign-in prompt, email address or password. The leaderboard
marks your own row with **(you)**.
The web app configuration is in `data/FirebaseClient.kt`. Analytics is not
initialized. There are no service-account credentials in the website.

## One-Time Console Setup

1. Open [the Firebase project](https://console.firebase.google.com/project/ballbuddy-97095/overview).
2. Open **Authentication**, click **Get started** if needed, then open
   **Sign-in method > Add new provider > Anonymous**. Enable it and click **Save**.
   You do not need to create player accounts manually. If Google was already
   enabled, it can stay enabled; this website no longer uses its sign-in flow.
3. If your project offers **automatic cleanup** of anonymous accounts, leave it
   **disabled** so returning players do not lose their identity after 30 days.
   This option is available with Firebase Authentication with Identity Platform.
   See [Firebase's anonymous authentication guide](https://firebase.google.com/docs/auth/web/anonymous-auth).
4. In **Firestore Database > Create database**, choose **Standard edition**,
   the `(default)` database, production rules, and a suitable European location.
   The functions use `europe-west1`; keeping the database nearby reduces latency.
5. Upgrade the project to **Blaze** to deploy Cloud Functions. Configure a budget
   alert in Google Cloud Billing; alerts do not cap charges. Functions use zero
   minimum instances and a maximum of three instances per function.

Firebase Hosting is not required. Neither Cloud Storage nor uploaded media is
needed for this step; the existing images and videos still come from the site.
The public web app configuration does not grant permission to change project
settings. Complete these steps while signed in to the account that owns the
Firebase project.

## Deploy the Backend

Use Node.js 22 or newer. From the `8BallPatterns` directory:

```powershell
npm --prefix functions ci
npm --prefix functions test
npx firebase-tools login
npx firebase-tools deploy --project ballbuddy-97095 --only 'functions,firestore'
```

Check that the browser login uses the Google account that owns this Firebase
project. Firebase CLI authentication stays on your machine. No private key needs
to be pasted into chat or added to GitHub.

The deploy hook generates the private answer catalog from the shot folders.
The cloud build uses that uploaded catalog rather than rebuilding it without
the website's source folders.
Deploying functions after adding shots makes them eligible for subsequent daily
rounds. Existing daily shot sets are immutable, so keep the current day's media
available when deploying the frontend.

Do not delete shots used by a saved daily challenge. Set `"archived": true` in
their `meta.json` instead, retaining the original ID, layout, and clip. Archived
shots remain in the public manifest for frozen challenges, but are hidden from
browsing and excluded from newly generated backend catalogs/rounds. Never reset
a live round to repair missing media: restore the referenced assets instead.

If videos must be withdrawn, remove their published assets. The generated
`published-shot-ids.json` lets the backend reject new starts/answers for a saved
round whose media is missing, with a clear explanation. Existing round data,
attempts and scores remain unchanged; subsequent UTC dates use the active catalog.
Never restore withdrawn media merely to keep a past round playable.

Commit and push the frontend changes to deploy GitHub Pages through the existing
workflow. Backend deployment is a separate command, not part of the Pages build.
The public Firebase config does not require GitHub secrets.
Deploy the backend before publishing the updated frontend; it now calls
`getDailyChallengeStatus` to restore the saved alias and today's progress.
If you deployed the previous Google-sign-in version, run the deploy command
again. New player profiles are created automatically; no database reset is needed.

## Player Names and Return Visits

Welcome leads directly into the daily round, retaining player introductions.
There is no mode-choice or nickname-entry reel. Anonymous sign-in and a unique
server-assigned adjective/noun name happen automatically while welcome loads.
Normal return visits always show welcome. Completed-today identities then continue
into practice; unfinished or new UTC rounds continue into the challenge. The practice header's trophy opens
the round/overview and Choose player opens the optional practice selector.

The results page's Rename button validates and reserves a 3–20-character name.
Reservations are case-insensitive and retained after a rename. The authenticated
user ID owns names, attempts and totals; a rename never creates another attempt,
changes an answer or transfers points. Current profile names are joined into
daily/all-time standings so historical names do not get stuck in snapshots.

Clearing browser data, another browser/device or private sessions can lose the
anonymous identity. No account recovery or automatic cross-device sync is provided.

## Daily-First Rollout and Historical Backfill

Deploy Functions and the new Firestore indexes before publishing the frontend.
Wait for all three new indexes to become ready: `results` (simulated, correctCount)
for rank counts, `results` (simulated, correctCount, totalElapsedMs) for standings,
and `challengerTotals` (correctCount DESC, __name__ ASC) for all-time standings.
The last index is explicit because its document-ID ordering differs from the
descending score direction. Existing index definitions are retained.

Backfill legacy name reservations and real completed scores using Admin SDK
Application Default Credentials (for example, a local `gcloud auth
application-default login` with an authorized account). Run explicitly:

```powershell
npm --prefix functions run backfill:all-time -- ballbuddy-97095
```

This command is NOT run by builds, deployments or browser requests. It requires
an explicit project ID. Review the target before running it; local tests only
use `demo-ballbuddy`. It reserves existing profile names, assigning a new
automatic name where a legacy duplicate conflicts. It then credits historical
real result documents, marking each as counted and setting simulated=false.
Existing simulated results are retained but never ranked or credited.

Backfill and new completions use the same transactional accounting. Repeating
the backfill does not double-count scores, including if new users finish while
it runs. Deploy/backend, backfill and then publish/frontend; do not reset
existing challenges, attempts or totals.

## Data and Rules

- `dailyChallenges/{UTC-date}`: frozen daily shot IDs and answer keys.
- `players/{uid}`: current validated name and profile timestamps.
- `playerNames/{normalized-name}`: permanent UID-owned name reservations.
- `namePools/default`: remaining fixed combinations and numbered fallback.
- `dailyChallenges/{UTC-date}/attempts/{uid}`: server-clock starts and answers.
- `dailyChallenges/{UTC-date}/results/{uid}`: completed score, time,
  simulated=false and countedInAllTime marker.
- `challengerTotals/{uid}`: total correct, total questions and completed rounds.

All direct browser Firestore access stays denied. Authenticated callables own
profile changes, answer scoring, totals and standings. The public status
endpoint reveals only UTC date/count unless authenticated. The server generates
new rounds exclusively from challengeOnly=true, non-break shots; all active
answer mappings remain in the private catalog to support old frozen rounds.

The last accepted answer creates the daily result and credits all-time totals
in the same transaction. Retries return the accepted answer without crediting
again. Partial rounds do not contribute to all-time totals.

Daily ranks use correct answers descending, then server answer time ascending.
All-time ranks use cumulative correct answers only; equal totals share a rank.
Both APIs expose real results only and hydrate current names. The compact UI
shows top three plus your own row; pagination remains supported by the APIs.

This remains a casual anonymous leaderboard: public media/metadata can expose
answers, and resetting browser data can create another identity. It is not an
anti-cheat or high-stakes competition system.

## Local Firebase Emulators

Emulators use the isolated `demo-ballbuddy` project. Nothing is written to the
live project. Java 21 or newer is required for the Firestore emulator.

```powershell
npm --prefix functions run build
npx firebase-tools emulators:start --project demo-ballbuddy --only auth,firestore,functions
```

Start Kobweb separately. At `http://localhost:8080`, run this once in the browser
developer console, then reload:

```javascript
localStorage.setItem("ballbuddy-firebase-emulators", "true");
location.reload();
```

The flag only works on `localhost` or `127.0.0.1`. Starting a challenge creates an
anonymous account in the Auth emulator without a popup. Remove the localStorage
item and reload to return to the real Firebase project.

For the backend integration test (with the emulators stopped):

```powershell
npx firebase-tools emulators:exec --project demo-ballbuddy --only 'auth,firestore,functions' "npm --prefix functions run test:emulator"
```

This verifies automatic/concurrent name reservation, collision rejection,
rename persistence, reserved non-break eligibility, immutable daily rounds,
server timing, concurrent answer retries, idempotent lifetime totals/backfill,
real daily/all-time ranks, pagination/ties and direct-write denial.
