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

The experiment is disabled and has no navigation entry. The code is retained
for later: enable `ENABLE_YOUTUBE_EXPERIMENT` and add a navigation link to
resume at `/youtube-test`.
The isolated experiment reuses Rob Hall's player introduction,
layout image, image zoom and ball picker for `AMDgls4nJhQ/052056`.
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
set `ENABLE_YOUTUBE_EXPERIMENT = false` in `model/YouTubeExperiment.kt`; this
disables the test route. To remove it completely, delete the
YouTube experiment's page, model, player bridge, reveal component and tests,
remove any added navigation link/import, and make `EightBallReelLayoutOverlay` private again.

## Branding

The 8-ball logo with its mint ring and dots is in
`site/src/jsMain/resources/public/images/pool-ball-icon.svg`. The opening reel
and browser favicon share this asset. It uses flat colors and outlined artwork
so it stays sharp without external fonts or image services.

## Shot Data

### 9-ball support

The catalog also supports `"discipline": 9` shots. Omitted discipline means
8-ball, preserving existing data and links. A 9-ball answer is `"quarter": 1`
(top-left), `2` (top-right), `3` (bottom-left), or `4` (bottom-right), independent
of `group` and `ball`. Metadata must include a reviewed `tableSelection` with
`schemaVersion: 1`, four normalized `corners` ordered top-left, top-right,
bottom-right, bottom-left, and the final image's width, height and SHA-256.

The tools workspace's annotation editor supports this discipline without ball
detection. It shows perspective-correct quarters and saves the selected landing
quarter. See `tools/shot_annotation/README.md` in that workspace. Import reviewed
shots only. Five reviewed Gangfløt–Nordby shots are included under
`public/8ball/OBK/Wqc5orC1jvs/emil_andre_gangflot_vs_jack_nordby/`, with their
saved quarter answers and practice flags preserved.
Scratches/off-table finishes have no valid quarter and must not be imported.

In practice and daily challenges, players select a quarter on the photograph,
then reveal. Dotted dividers and a light highlight stay aligned during zoom.
Player descriptions show a 9-ball and “9-ball”; feedback uses the existing
thumbs-up/down and names the landing quarter. Daily answers use the existing
numeric answer transport, validated as quarters 1–4 server-side. Rebuild and
deploy the function catalog with the frontend when publishing reviewed shots.
The existing brand and `/8ball` asset paths are retained.

### Shared shot storage

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
Non-break `"ball": null` shots are excluded from the website; the numbered picker
no longer offers **No ball**. Missing `ball` metadata is not a no-pot answer.
Breaks still use `ballsPotted` and the count picker instead.
The build generates the shot manifest from complete shot directories.
Links such as `/?shot=AMDgls4nJhQ%2F051958` open a specific shot directly.

### Ball selection and annotation

The tools repo's `tools/annotate_shots.py` serves the editor at
`http://127.0.0.1:8766`. It proposes ball regions on the final `layout.jpg`, lets
the author correct them, mark the answer and choose its number, then saves
`ballSelection` plus `ball` into metadata. Media is unchanged. See the tools
repo's `tools/shot_annotation/README.md` for the schema and editing workflow.

In **Just browse and daily challenges**, reviewed ball regions become selectable
ball buttons with a one-time selection pulse, a steady outline and optional
vibration. Confirmation shows a short animated thumbs-up/down and synthesized
sound before the reveal; the speaker button mutes sounds. Reduced-motion
preferences disable pulse/pop animations. Daily ball IDs are scored by the
server, including frozen rounds created before annotations. Deploy the updated
functions/catalog before releasing the frontend. Older numbered clients remain
supported. Image zoom and panning keep the hit areas aligned. Ambiguous overlapping
targets require a clearer tap. Unannotated shots keep numbered buttons; breaks
keep 0–4 counts. The local editor's
`annotationBalls` preview link tests a draft immediately on the modified local
website without rebuilding; saved catalog changes need a resource rebuild.
Non-break shots with `ballSelection` use annotated regions; break shots remain counts.
Existing folders suffixed `_review` are pending another editorial review, not disabled.

Double-tap or double-click a layout to enlarge just the image in its existing
frame. The enlarged image fills the available height (at least 1.5 times the
fitted size), cropping the sides on phones. Drag with one finger or the left mouse
button to inspect hidden areas; another double-tap/click restores the full image.
Panning stops at the image edges. Zoom gestures and drags do not select balls;
single taps select after a brief delay to distinguish them from double taps.
The hint below the image explains the gesture. Keyboard users can focus the
image, press Enter/Space to toggle zoom, use arrow keys to pan, and Escape to
zoom out. Page pinch zoom is disabled inside the reels; normal vertical swiping
still works outside the enlarged image. Zoom resets when changing shots.

### Installation and navigation

The welcome page offers **Install app**. Supported browsers show their native
installation prompt; iPhone/iPad users get Add to Home Screen instructions.
The web manifest uses relative URLs so repository-base-path deployments work.
PNG icons are rendered from the existing vector logo. There is no service
worker or offline video cache: installed users still need internet, and receive
normal website updates. Unsupported browsers can continue using the website.
Desktop users also get circular previous/next reel buttons. Player introductions
show identity, ball group and ball-in-hand status first, with Fargo rating,
match year and photo attribution under **More details**.

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

In practice, **Choose player** in the header opens a selector offering **Any player** or the shooting
players actually represented by active published shots. The list does not come
from the complete `players.json` profile directory, and excludes opponent-only
or archived-only names. The shuffle remains stable during navigation and
rendering, and a fresh page load gets a fresh seed.
Player cards show `Fargo <rating>` from `players.json` when available, with no
detail line otherwise. Choosing a player puts all of that player's shuffled
groups first, then continues with shuffled groups from the remaining players.
Every active shot appears once before the feed loops; groups stay intact in
both sections. Daily challenges are unaffected.
Browsing shows the player introduction only at each group's start, including
when the feed loops. There is no intermediate rack notice.
`matchYear` in a match's `source-info.json` is inherited by its shots and shown
discreetly as **Match · 2023** beneath the player's name. Omit it if unknown;
a shot's own `matchYear` can override the match default.
Within a group, a completed reveal automatically opens the next layout. At the end of
a group, it automatically scrolls directly to the next group's player
introduction. Manual forward/back navigation
also skips introductions inside runs. Daily shots remain independent and retain
their individual introductions and explicit Next shot / See results controls.

Backward navigation returns from a group's player
introduction directly to the preceding shot. Manual input takes priority over
automatic advancement, including when scrolling backward as a reveal finishes.
The three physical page slots stay fixed; recycling their content happens with
scroll snapping briefly suspended so recentering cannot trigger another page
change. Cached layouts and portraits remain visible when previews become active.
Mouse-wheel notches advance a full page even when OS settings produce a small
pixel delta; high-resolution wheel gestures retain native scrolling and snapping.

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

Erik Gårdelöf, Emil André Gangfløt, and Kim Soo Garmark use locally stored CueScore portraits
(reuse permission confirmed by the site owner). Their profile links are recorded
in `players.json`; the introduction keeps a CueScore credit link. Optional
`country` and `countryFlagUrl` fields display a small accessible national flag
beside the name. CueScore lists Erik as Sweden and Emil as Norway. Portraits and
flags are served locally, not hotlinked. Original portrait downloads:

- Erik: https://img.cuescore.com/image/e/4/e1266df8dfc9edb21a9816bb8c812135.png?t=1569668287
- Emil: https://img.cuescore.com/image/9/4/963ffedb0da7bc8fa83237386abda13b.png?t=1771601925
- Flags: CueScore `/img/flags/png-country-4x2-none/res-40x30/se.png` and `no.png`.

Jayson Shaw and Albin Ouschan also use locally stored portraits (reuse permission
confirmed by the site owner). Original downloads:

- Jayson: https://worldnineballtour.com/wp-content/uploads/Jayson-Shaw-4.png
- Albin: https://worldnineballtour.com/wp-content/uploads/albin-ouschan_profile.webp

All five portraits are tracked under `site/src/jsMain/resources/public/images/players`.
`imageUrl` selects the displayed local image; `imageSourceUrl` is only the photo
credit link in More details and does not fetch the displayed portrait.

`site/src/jsMain/resources/public/players.json` maps player names from shot
metadata to `imageUrl`, `imageCredit`, `imageSourceUrl`, and `imagePosition`.
Names are matched without regard to case. Use an HTTPS image URL or a local
public path such as `/images/players/jayson-shaw.jpg`.
Portraits are centered by default; `imagePosition` can override the focal point.
Optional `FargoRating`, `FargoSource`, and `FargoDate` fields are shown only for
the featured player on the introduction. The rating is prominent; source/date
are secondary. Missing photos do not prevent ratings displaying.
Open-table introductions show all fourteen solids and stripes in two rows.

Unknown players and failed images fall back to a person icon without blocking
the feed.

Reel buttons share a brief visual press response and a 10 ms haptic pulse on
browsers/devices that support vibration. Disabled buttons do not respond. Reduced
motion keeps the brightness response but omits the press scaling and shared
vibration. Player cards and mode choices share the same hover colors. Ball
selection retains its existing dedicated haptic feedback.

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
Automatic shot-to-shot advancement waits for the next layout image to load and
decode; manual navigation can still interrupt it. The loading indicator overlays
the table instead of resizing its header. Every newly displayed active shot image
starts at the existing zoomed size and eases back to fitted size over two seconds,
including first shots, manual swipes, and daily challenges. Incoming previews stay
still at the zoomed starting size, including before image loading/measurement,
so scrolling never shows a fitted image before zooming out. Reduced-motion keeps
previews fitted and disables the animation, and interacting
with the image pauses it; manual zoom takes over normally. Ball hit testing uses
the transformed image bounds so selections remain aligned during the pulse.

## Daily Challenge

The main entrance is daily-first. Keep the welcome reel, then go directly to
the day's player introductions and shots, without mode selection or nickname
entry. Every unanswered challenge shot retains its player introduction. Saved
partial rounds resume after the welcome; the final reveal automatically opens
results. Normal visits always open the welcome reel. A browser that completed
today's round continues from welcome into practice; unfinished or new UTC rounds
continue into the challenge. Shared shot links still open their specific shot.

Practice has no mandatory player picker. **Choose player** in its header opens
the existing selector; Back to shots returns to the same position. The trophy
opens today's challenge or its saved overview. Challenge mode has no player
picker. Shared practice-shot links still open their exact shot.

### Reserved challenge shots

Every active shot has an explicit editorial boolean `challengeOnly` in `meta.json`.
Any non-break shot may be tagged, regardless of its position in a group. The
earlier tagging of group starters was a one-time curation, not an import rule.
All breaks must remain false. Group boundaries
are established on the full catalog before removing reserved shots, so removing
a whole opponent turn cannot merge adjacent runs. Groups left empty are omitted.

Only tagged non-break shots are eligible for new daily rounds. Practice excludes
all challenge-only shots, including from its player list. Imported shots need
explicit tags; imports preserve the author's choices without retagging a group.
The backend validates boolean tags and rejects challenge-only breaks.

Folders may be renamed with `_review` while preserving their original explicit
`id` in `meta.json`. Rebuild resources to update media paths. Stable IDs preserve
shared `?shot=` links and frozen daily rounds; the suffix does not hide shots.

Each UTC date gets a server-defined set of up to three distinct eligible shots,
frozen at the first start. Everyone gets the same round. Sets may repeat across
dates; this release does not promise an unseen daily set forever. Keep original
media and answer data for existing frozen rounds, including old break/practice
rounds selected before this feature. Catalog filtering only affects new rounds.

### Names, results and all-time standings

Firebase silently creates a persistent anonymous browser identity. The server
reserves a unique name drawn from a fixed adjective/noun pool, such as Red Tiger
or Silent Falcon. All combinations are consumed before adding numbered names.
Claims are case-insensitive, transactional, and retained after renaming.
Names remain tied to the user ID, not the device hardware or the alias string.

Results fit on one screen: score/time, a labelled **Your leaderboard name** card
with **Change name**, Today/All time standings (top three plus the user's own row),
a highlighted **New challenge tomorrow** banner, and a **See more shots** button
with a swipe cue into practice. Desktop content is capped at 440px. There is no
per-shot breakdown. Inline name editing focuses/selects the current name, provides
Save name/Cancel, and prevents accidental reel navigation while editing. A brief
Name updated confirmation follows a successful save. The first result after this
browser receives an automatic name explains that it can be personalised; the
one-time hint is stored per anonymous identity in browser storage. Editing and
error states may scroll vertically on small screens or with the keyboard open.
Renaming saves through an authenticated callable and is reflected in both
leaderboards, including older results, without resetting the attempt or score.
Names use the existing 3–20-character validation.

Daily standings rank correct answers descending, then answer time ascending.
All-time standings rank total correct answers across completed challenges;
equal totals share a rank. Completion and its all-time increment are written
atomically, and retries cannot score a round twice. Simulated results are no
longer generated or ranked. An idempotent backfill reserves legacy names and
credits historical real results; see [FIREBASE.md](FIREBASE.md).

Shot timing remains server-authoritative and starts only when the layout is
ready. Challenge requests have explicit loading/retry states. Clearing browser
data or using another browser can lose the anonymous identity; typing the same
name cannot recover it.

**Rollout:** deploy the new Functions and Firestore index and run the historical
backfill before publishing this frontend. No production deployment or migration
is performed by a local build/test.

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
