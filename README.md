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

## Branding

The 8-ball logo with its mint ring and dots is in
`site/src/jsMain/resources/public/images/pool-ball-icon.svg`. The opening reel
and browser favicon share this asset. It uses flat colors and outlined artwork
so it stays sharp without external fonts or image services.

## Shot Data

Shots live under `site/src/jsMain/resources/public/8ball/<match>/<shot>/`.
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
The build generates the shot manifest from complete shot directories.
Links such as `/?shot=BmbUbSQ8av4%2F000346` open a specific shot directly.

The feed alternates a player introduction and a shot. Add `&view=player` to a
shot link to open its introduction. The shot clock counts visible-tab study
time after the layout image loads, and resets when entering another attempt.

## Player Photos

`site/src/jsMain/resources/public/players.json` maps player names from shot
metadata to `imageUrl`, `imageCredit`, `imageSourceUrl`, and `imagePosition`.
Names are matched without regard to case. Use an HTTPS image URL or a local
public path such as `/images/players/jayson-shaw.jpg`.
Portraits are centered by default; `imagePosition` can override the focal point.
Open-table introductions show all fourteen solids and stripes in two rows.

The initial registry links to the players' World Nineball Tour profile images
and includes source links. These are external references, not a grant of image
reuse rights. Use photos approved for your public site. Unknown players and
failed images fall back to initials without blocking the feed.
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

## Daily Challenge

After the welcome reel, choose **Daily challenge** or **Just browse**. Browsing
does not require an account. The Home button beside Like and Share returns to
the choice reel. Choosing Daily challenge opens a separate nickname reel where
you can enter a leaderboard alias and start, or return to the choices. Firebase
silently creates an anonymous identity; no Google account or password is needed.
Complete the one-time setup in [FIREBASE.md](FIREBASE.md) before trying a ranked round.

Each UTC date gets a server-defined set of up to ten distinct shots, frozen when
the first participant starts. The current catalog has three shots, so the round
has three. Every player ID has one attempt per date, resumable in the same browser.
Duplicate aliases are allowed. Names are remembered and fixed for each started
daily attempt. A new browser or cleared site data creates a different identity;
entering the same alias cannot restore the original account or progress.
Results from the older browser-only prototype are not uploaded or ranked.

The server starts each shot clock when the layout is ready. Time runs until the
answer reaches the server, including hidden tabs and interruptions after that
shot starts. Introductions and reveals between shots do not count. The clock
cannot be reset by reloading. The displayed clock is approximate; the result
uses server time. Challenge shots cannot be skipped or answered twice.

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
