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
