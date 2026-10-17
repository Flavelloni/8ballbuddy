# Firebase Setup

The website stays on GitHub Pages. Firebase project `ballbuddy-97095` supplies
anonymous player identities, private challenge progress, callable functions,
and standings. Players choose an alias; there is no Google sign-in prompt,
email address, password, or visible account tag. Duplicate aliases are allowed,
and the leaderboard marks your own row with **(you)**.
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

Commit and push the frontend changes to deploy GitHub Pages through the existing
workflow. Backend deployment is a separate command, not part of the Pages build.
The public Firebase config does not require GitHub secrets.
Deploy the backend before publishing the updated frontend; it now calls
`getDailyChallengeStatus` to restore the saved alias and today's progress.
If you deployed the previous Google-sign-in version, run the deploy command
again. New player profiles are created automatically; no database reset is needed.

## Player Aliases

On the choice reel, enter a **Leaderboard name** and start the daily challenge.
The first start silently creates a Firebase anonymous account. Names use 3 to 20
characters: letters, numbers, spaces, underscores, or hyphens. The server
normalizes and validates the alias before saving it. There is no name reservation
or uniqueness check, so two people can both choose `PoolFan42`.

The player ID owns the attempt, not the name. An alias is fixed once that day's
attempt starts, and the name field becomes read-only for the rest of the round.
Renaming a request cannot reset an attempt or relabel an existing result.
Before starting a new day's challenge, the remembered alias can be edited.

Returning in the same browser restores the alias and any started or completed
attempt. There is no guest sign-out button, since signing out would lose access
to that anonymous identity. Clearing site data, using another browser/device,
or closing a private-browsing session can lose the identity. Existing scores
remain on the leaderboard, but entering the same alias does not recover them.
There is no account recovery or automatic cross-device sync in this version.

After deployment, test the website by entering an alias, starting a round, and
reloading. The same name and progress should return. A separate browser profile
can choose the same name and should get its own attempt and **(you)** marker.

## Data and Rules

- `dailyChallenges/{UTC-date}` holds the frozen daily shots and answer key.
- `players/{uid}` holds the player's validated alias for future daily rounds.
- `dailyChallenges/{UTC-date}/attempts/{uid}` holds each account's progress and
  server-recorded clock starts and answers.
- `dailyChallenges/{UTC-date}/results/{uid}` contains completed results with a
  display name, score, total time, and completion time. Emails are omitted.
- The first standings request also saves three date-seeded, clearly labeled
  simulated results. Their names come from a pool of ten; no simulated shot takes
  less than 60 seconds, and none has a perfect score. A marker prevents duplicates.

All browser Firestore access is denied. Authenticated callable functions handle
reads and writes through the Admin SDK. Clients submit shot IDs and chosen balls,
never scores or answer times. Answer submission and result publication happen in
one transaction. Repeated requests return the accepted answer without scoring it
twice. The functions do not send unsubmitted answers to the challenge client.

Standings sort by correct answers descending, then total shot time ascending.
Exact ties share a rank, including across page boundaries. Simulated results
count in these ranks and are labeled in the table. The results reel can load
every entry, and your own rank is calculated even when your row is on a later page.

Public browsing metadata and reveal clips still expose answers to someone who
inspects the site. One Firebase player ID is one daily entry, not proof of a
unique person. Resetting browser storage can create another identity and allow
extra attempts. Server scoring prevents direct score forgery but does not make
this guest leaderboard suitable for a high-stakes competition.

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

This verifies anonymous authentication, alias validation and restoration,
duplicate names with separate identities, immutable daily rounds and aliases,
server timing, concurrent submission retries, publishing completed attempts,
pagination and ties, and Firestore rules rejecting direct profile/result writes.
