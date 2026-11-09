# 8-Ball Buddy

A single-page Kobweb app migrated from the `/8-ball-prediction-reels` page in
`kobweb-site`. The reels experience is now the home page at `/`.

Page state and composition live in `pages/Index.kt`. UI components are grouped
under `components/reels`, with shot models and prediction rules in `model`,
loading in `data`, and URL and sharing helpers in `browser`.

## Development

From this directory:

```powershell
cd site
kobweb run
```

Open http://localhost:8080. The server port is configured in `site/.kobweb/conf.yaml`.

## Parked YouTube experiment (disabled)

The experiment is disabled and its mode-selection link is commented out. The
code is retained for later: enable `ENABLE_YOUTUBE_EXPERIMENT` and uncomment the
link in `ChallengeReels.kt` to resume at `/youtube-test`.
The isolated experiment reuses Rob Hall's player introduction,
layout image, fullscreen image viewer and ball picker for `AMDgls4nJhQ/052056`.
Choosing a ball automatically reveals a muted YouTube player for broadcast
seconds **19300.4–19308.5**, equivalent to the existing MP4's 8.1-second clip.
The iframe uses supported `controls=0` and `disablekb=1` settings. At the end of
the shot it is destroyed and replaced by our own end view; **Replay shot** creates
a new player with the same start/end bounds, avoiding YouTube's whole-video replay.
The complete player uses the available width without cropping, with an external
fullscreen button where the browser supports it. Play/pause, retry, original-source and
MP4-comparison controls remain outside the player. Ads, branding, buffering and
browser playback restrictions remain genuine parts of this test; no ad blocking,
cropping or hidden/background YouTube playback is used.

The regular shot catalog, daily challenge and existing MP4s are unchanged. YouTube
is contacted only when a reveal is requested on this route. To disable the test,
set `ENABLE_YOUTUBE_EXPERIMENT = false` in `model/YouTubeExperiment.kt`; this hides
the menu link and disables the test route. To remove it completely, delete the
YouTube experiment's page, model, player bridge, reveal component and tests,
remove its menu link/import, and make `EightBallReelLayoutOverlay` private again.

## Branding

The 8-ball logo with its mint ring and dots is in
`site/src/jsMain/resources/public/images/pool-ball-icon.svg`. The opening reel
and browser favicon share this asset. It uses flat colors and outlined artwork
so it stays sharp without external fonts or image services.

## Shot Data

Shots live under
`site/src/jsMain/resources/public/8ball/<broadcaster>/<video-id>/<matchup>/<shot>/`.
Each shot directory contains `meta.json`, `layout.jpg`, and `clip.mp4`.
Metadata uses the same format as the original site:

```json
{
  "player": "Jayson Shaw",
  "opponent": "Albin Ouschan",
  "group": 1,
  "ball": 6
}
```

`group` is `0` for an open table, `1` for solids, and `2` for stripes.
For non-break shots, explicit `"ball": null` means no ball was potted. The
picker always offers **No ball**, scored internally as answer `0` (also accepted
by the daily-challenge backend). A missing `ball` is not treated as a no-pot answer.
Breaks still use `ballsPotted` and the count picker instead.
The build generates the shot manifest from complete shot directories.
Links such as `/?shot=AMDgls4nJhQ%2F051958` open a specific shot directly.

Shot IDs remain `<video-id>/<timestamp>` even when files move into broadcaster
and matchup folders. This preserves shared links and frozen daily challenges.
The existing MagicCitySlate shots have `legacyAssetPath` entries so the build
also generates copies at their old asset URLs for already-open/cached clients;
these aliases are build output, not duplicate source files in Git.

### Import a reviewed match folder

Copy the local broadcaster folder (for example `OBK/`) into
`site/src/jsMain/resources/public/8ball/`. The build publishes only shot folders
containing `meta.json`, `layout.jpg` and `clip.mp4`; folders suffixed `_bad` and
hidden working folders are ignored. A folder with `meta.json` but missing media
fails the build so incomplete reviewed shots are not silently omitted.

Only those three presentation files are published. Full recordings, preserved
`screenshot.jpg` images, timing reports, contact sheets and unannotated shots
are excluded, even if the complete local match folder was copied. Unreviewed
shots can stay alongside the reviewed ones and become available after you add
their `meta.json` files. Do not commit raw source videos to the website repository.

Put `source-info.json` alongside the match's timestamp folders with `source` and
`videoLink`. Missing credits in each `meta.json` are inherited from it in the
generated public metadata. You only need to author the player and shot answer.
The backend catalog uses the same stable shot IDs and skips `_bad` folders.

Browsing forms chronological shot groups, then shuffles whole groups once per
page load. A break, different shooting player, different match/video, or gap
greater than three minutes starts a new group. Exactly three minutes does not.
Changing ball assignment from 1→2, 2→1, 1→0 or 2→0 also starts a group;
assigning solids/stripes from an open table (0→1/2) does not.
Shots inside a group retain chronological order. Grouping happens before player
prioritization, so putting a player first never merges their separate turns.
This supersedes the older `browseOrder` priority; no folder prefixes or resource
reorganization are required. Shared shot IDs are unchanged, and shared links
still open their exact shot within the page's shuffled order.

After **Just browse**, a player-choice reel offers **Any player** or the shooting
players actually represented by active published shots. The list does not come
from the complete `players.json` profile directory, and excludes opponent-only
or archived-only names. The shuffle remains stable during navigation and
rendering, and a fresh page load gets a fresh seed.
Player cards show `Fargo <rating>` from `players.json` when available, with no
detail line otherwise. Choosing a player puts all of that player's shuffled
groups first, then continues with shuffled groups from the remaining players.
Every active shot appears once before the feed loops; groups stay intact in
both sections. Daily challenges are unaffected.
Browsing shows the player introduction only at each group's start. For each
new group after the first, a **New rack** reel precedes that introduction.
The first group opens directly with the player, and daily shots do not show rack notices.
`matchYear` in a match's `source-info.json` is inherited by its shots and shown
discreetly as **Match · 2023** beneath the player's name. Omit it if unknown;
a shot's own `matchYear` can override the match default.
Within a group, a completed reveal automatically opens the next layout. At the end of
a group, swipe to the next group's introduction. Manual forward/back navigation
also skips introductions inside runs. Daily shots remain independent and retain
their individual introductions and explicit Next shot / See results controls.

`"ballInHand": true` in a shot's `meta.json` displays **Ball in hand** on both
the player introduction and unanswered layout. Missing/false preserves the
ordinary display. The cue ball is not draggable and the answer remains the
object ball; this is an explanatory notice only.

Match-specific ball colors can also be set once in that `source-info.json`:

```json
"ballColors": { "4": "#F4A6C1", "12": "#F4A6C1" }
```

The build inherits this map into each shot's public metadata. It changes the
4 ball and 12 ball's stripe to light pink in both the introduction and picker,
without changing the standard palette for other matches. Individual
shot `meta.json` files can supply their own `ballColors` map (`{}` restores the
standard palette). Keys are ball numbers 1–15; values must be `#RRGGBB` colors.
There is no broadcaster-name hardcoding: a different OBK match can use its own
palette. For a non-timestamp folder name, set an explicit stable `id`, for
example `"id": "QymvyRiXJ9A/001412"` in `001412_emil/meta.json`.

For a break, set `isBreak` and the actual number of balls potted instead of a
target ball. The picker offers plain counts 0 through 4; zero is a valid answer:

```json
{
  "player": "Lance Ireland",
  "opponent": "Rob Hall",
  "group": null,
  "ball": null,
  "isBreak": true,
  "ballsPotted": 0,
  "source": "MagicCitySlate",
  "videoLink": "https://www.youtube.com/watch?v=AMDgls4nJhQ"
}
```

Ordinary shots can also specify `source` and `videoLink` for a credit beneath
the video reveal. Layout JPGs are displayed as supplied, without rotation.
Both browse scoring and the private daily-challenge catalog support break
counts; regenerate the catalog and deploy updated Functions with frontend
changes when publishing this new question type (see `FIREBASE.md`).

The feed alternates a player introduction and a shot. Add `&view=player` to a
shot link to open its introduction. The shot clock counts visible-tab study
time after the layout image loads, and resets when entering another attempt.

## Player Photos

Erik Gårdelöf and Emil André Gangfløt use locally stored CueScore portraits
(reuse permission confirmed by the site owner). Their profile links are recorded
in `players.json`; the introduction keeps a CueScore credit link. Optional
`country` and `countryFlagUrl` fields display a small accessible national flag
beside the name. CueScore lists Erik as Sweden and Emil as Norway. Portraits and
flags are served locally, not hotlinked. Original portrait downloads:

- Erik: https://img.cuescore.com/image/e/4/e1266df8dfc9edb21a9816bb8c812135.png?t=1569668287
- Emil: https://img.cuescore.com/image/9/4/963ffedb0da7bc8fa83237386abda13b.png?t=1771601925
- Flags: CueScore `/img/flags/png-country-4x2-none/res-40x30/se.png` and `no.png`.

`site/src/jsMain/resources/public/players.json` maps player names from shot
metadata to `imageUrl`, `imageCredit`, `imageSourceUrl`, and `imagePosition`.
Names are matched without regard to case. Use an HTTPS image URL or a local
public path such as `/images/players/jayson-shaw.jpg`.
Portraits are centered by default; `imagePosition` can override the focal point.
Optional `FargoRating`, `FargoSource`, and `FargoDate` fields are shown only for
the featured player on the introduction. The rating is prominent; source/date
are secondary. Missing photos do not prevent ratings displaying.
Open-table introductions show all fourteen solids and stripes in two rows.

The initial registry links to the players' World Nineball Tour profile images
and includes source links. These are external references, not a grant of image
reuse rights. Use photos approved for your public site. Unknown players and
failed images fall back to a person icon without blocking the feed.
No open reuse licence has been verified for the WNT photos. Their profile pages
carry an "all rights reserved" notice; obtain appropriate permission or replace
the photos with assets licensed for your intended website use before publishing.

Layout containers use Kobweb Compose wrappers. Native HTML elements remain for
headings, paragraphs, buttons, images, and video to preserve their semantics and
media behavior.

## Media Caching

Shot images, clips, and metadata use content-hashed version URLs generated during
the build. Unchanged files retain the same URL; replacing a file changes its URL
automatically. The shot manifest is revalidated on each page load. Media uses the
browser's normal HTTP cache and native video range requests, without downloading
the entire library. GitHub Pages currently serves media with a ten-minute cache
lifetime; this does not provide guaranteed offline playback or permanent storage.
The current layout is warmed ahead of display. Once a reveal starts, only the
next layout image is prefetched; the next video is not. Daily prefetch stops
at the final shot. Leaving a reveal pauses its video without rewinding it,
avoiding a flash of the first frame before the next layout appears.

## Daily Challenge

After the welcome reel, choose **Daily challenge** or **Just browse**. Browsing
does not require an account. The Home button beside Like and Share returns to
the choice reel. Choosing Daily challenge opens a separate nickname reel where
you can enter a leaderboard alias and start, or return to the choices. Firebase
silently creates an anonymous identity; no Google account or password is needed.
Complete the one-time setup in [FIREBASE.md](FIREBASE.md) before trying a ranked round.

Each UTC date gets a server-defined set of up to three distinct shots, frozen when
the first participant starts. The shot limit is defined only in the backend.
Shots are selected independently from the full active catalog using a date-seeded
shuffle: browsing groups, players, matches, and the browsing player filter do not
affect selection. Everyone gets the same daily round; there are no player-specific
daily challenges or separate player leaderboards. Only browsing has a player picker.
The nickname reel reads the actual shot count from Firebase without creating an
account or attempt. Every player ID has one attempt per date, resumable in the same browser.
Duplicate aliases are allowed. Names are remembered and fixed for each started
daily attempt. A new browser or cleared site data creates a different identity;
entering the same alias cannot restore the original account or progress.
Results from the older browser-only prototype are not uploaded or ranked.

The server starts each shot clock when the layout is ready. Time runs until the
answer reaches the server, including hidden tabs and interruptions after that
shot starts. Introductions and reveals between shots do not count. The clock
cannot be reset by reloading. The displayed clock is approximate; the result
uses server time. Challenge shots cannot be skipped or answered twice.
The stopwatch appears only on unanswered daily challenge layouts; browsing has no clock.

Completed rounds are published automatically. The results reel shows your rank,
all results (50 per page), and an answer breakdown. Ranking uses correct
answers descending and total answer time ascending; exact ties share a rank.
Three date-seeded simulated results provide comparison from the first round.
They use short names from a pool of ten, are labeled **simulated**, count in the
displayed ranks, never score perfectly, and take at least 60 seconds per shot.
Chosen aliases are public, with your own row marked **(you)**. There are no tags
or email addresses in standings. Aliases have 3 to 20 characters, using letters,
numbers, spaces, underscores, or hyphens.
Use Refresh standings to update the table as other players finish.

The server rejects forged scores and enforces answer order, but public browse
metadata and video URLs can reveal answers. This is a casual leaderboard, not
an anti-cheat competition. A competitive release needs separate unrevealed
challenge assets and controlled access to the reveals.

## Verification

```powershell
.\gradlew.bat :site:jsBrowserDevelopmentWebpack
.\gradlew.bat :site:jsBrowserTest
```

## Static Export

From `site`, run `kobweb export --layout static`.

## GitHub Pages

`.github/workflows/deploy-pages.yml` builds and deploys the static site on pushes
to `master`, or manually from the Actions tab. It uses the checked-in Gradle
wrapper and Java 25; installing the Kobweb CLI in CI is not necessary.

One-time setup:

1. Open the repository's **Settings > Pages**.
2. Under **Build and deployment**, select **GitHub Actions** as the source.
3. Commit and push these changes to `master`. Watch **Actions > Deploy GitHub Pages**
   for the deployment result and published URL.

For the current `Flavelloni/8ballbuddy` repository, the default address is
https://flavelloni.github.io/8ballbuddy/. No personal access token, `gh-pages`
branch, or separately purchased domain is required. The repository must be
eligible for GitHub Pages on your GitHub plan; public repositories can use it
on GitHub Free.

The workflow reads the actual Pages base path and applies it only in the CI
checkout before exporting. Local development stays at `/`; repository sites
use `/8ballbuddy/`. If you later configure a custom domain in Settings > Pages,
rerun the workflow so the generated paths match the new address. The uploaded
artifact is `site/.kobweb/site`, not the Kotlin source or the dev build.

To test a repository-path export locally, temporarily add `basePath: "8ballbuddy"`
under `site` in `site/.kobweb/conf.yaml`, then export and run:

```powershell
cd site
kobweb stop
kobweb export --layout static
kobweb run --env prod --layout static
```

Open http://localhost:8080/8ballbuddy/. Stop the server and remove the temporary
`basePath` before returning to normal development.

Before publishing publicly, clear the reuse rights for player portraits and
match footage, or replace them with content licensed for your site.
