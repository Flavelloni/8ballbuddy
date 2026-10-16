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

The original 8-ball and prediction-arrow mark is in
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

## Verification

```powershell
.\gradlew.bat :site:jsBrowserDevelopmentWebpack
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
