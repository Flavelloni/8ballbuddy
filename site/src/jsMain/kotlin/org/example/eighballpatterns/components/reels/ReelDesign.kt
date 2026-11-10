package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.px

@Composable
internal fun ReelBrandHeader(onHome: () -> Unit, enabled: Boolean, showSound: Boolean) {
    Row(Modifier.fillMaxWidth().attrsModifier { classes("reel-topbar") },
        verticalAlignment=Alignment.CenterVertically) {
        Button(onClick={onHome()},enabled=enabled,modifier=Modifier.attrsModifier {
            classes("reel-brand-home");attr("aria-label","8-Ball Buddy home");attr("title","Back to home")
        }) {
            Row(Modifier.gap(9.px),verticalAlignment=Alignment.CenterVertically) {
                Image(BasePath.prependTo("/images/pool-ball-icon.svg"),"",Modifier.width(28.px).height(28.px))
                SpanText("8-Ball Buddy")
            }
        }
        if(showSound) PredictionSoundToggle()
    }
}

// Shared visual tokens and responsive rules belong here rather than being
// duplicated across every reel. Structural UI remains Kobweb/Silk components.
internal val reelDesignStyles = """
    .eight-ball-app { --reel-mint: #ade7cb; --reel-muted: #95aea1; --reel-border: #ffffff12;
      font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      color: #edf6f0; -webkit-font-smoothing: antialiased; }
    .eight-ball-app button, .eight-ball-app input { font-family: inherit; }
    .eight-ball-app * { scrollbar-width: thin; scrollbar-color: #385442 transparent; }
    .eight-ball-app ::-webkit-scrollbar { width: 4px; }
    .eight-ball-app ::-webkit-scrollbar-thumb { background: #385442; border-radius: 8px; }
    .eight-ball-app ::-webkit-scrollbar-track { background: transparent; }
    .eight-ball-app button { -webkit-tap-highlight-color: transparent; transition: background .16s, border-color .16s, box-shadow .16s; }
    .eight-ball-app button:focus-visible, .eight-ball-app a:focus-visible { outline: 2px solid #ade7cb; outline-offset: 4px; }
    .reel-canvas { background: radial-gradient(ellipse at 50% 0%, #16342766, transparent 58%), #080f0c; box-sizing: border-box; }
    .reel-scrollport { scrollbar-width: none; overflow-anchor: none; overscroll-behavior-y: contain; }
    .reel-scrollport::-webkit-scrollbar { display: none; }
    .reel-page { position: relative; height: 100%; min-height: 100%; scroll-snap-align: start; scroll-snap-stop: always; overflow: hidden; }
    .reel-page-preview { pointer-events: none; }
    .reel-topbar { position: absolute; top: 0; left: 0; z-index: 30; min-height: calc(60px + env(safe-area-inset-top));
      padding: calc(8px + env(safe-area-inset-top)) 18px 8px; justify-content: space-between; box-sizing: border-box;
      background: #0a1410f2; border-bottom: 1px solid #ffffff0c; backdrop-filter: blur(16px); }
    .reel-brand-home.reel-brand-home { background: transparent; color: #edf6f0; border: 0; border-radius: 10px; padding: 4px 0;
      font-size: .89rem; font-weight: 750; letter-spacing: -.025em; min-height: 36px; }
    .reel-brand-home:hover { color: #ade7cb; }
    .reel-eyebrow { color: #a6d1bb; text-transform: uppercase; letter-spacing: .18em; font-size: .65rem; font-weight: 750; }
    .reel-primary-button.reel-primary-button { min-height: 50px; padding: 13px 22px; display: inline-flex; align-items: center; justify-content: center; gap: 10px;
      border-radius: 16px; border: 1px solid #c7f6dd55; background: #b0e6ca; color: #0b2618; font-size: .88rem; font-weight: 750;
      box-shadow: 0 4px 20px #0003; }
    .reel-primary-button.reel-primary-button:hover { background: #c5f2da; }
    .reel-primary-button > .kobweb-row { gap: 10px; }
    .reel-subtle-button > .kobweb-row { gap: 8px; }
    .reel-utility-button > .kobweb-row { gap: 6px; }
    .reel-primary-button.reel-primary-button:disabled, .reel-primary-button.reel-primary-button[aria-disabled=true] { background: #17261e; border-color: #ffffff10; color: #73897c; box-shadow: none; }
    .reel-subtle-button.reel-subtle-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 40px;
      background: #ffffff05; border: 1px solid #ffffff12; border-radius: 12px; color: #a9c3b5; font-size: .77rem; padding: 10px 14px; }
    .reel-subtle-button.reel-subtle-button:hover { background: #ffffff0b; border-color: #ade7cb44; }
    .eight-ball-reel-intro { gap: 22px; padding: max(28px, env(safe-area-inset-top)) 28px max(26px, env(safe-area-inset-bottom));
      background: radial-gradient(ellipse at 50% 20%, #254d363d, transparent 55%); }
    .eight-ball-reel-intro h1 { letter-spacing: -.06em; }
    .reel-hero-title { font-size: 2.55rem; }
    .eight-ball-reel-logo { filter: drop-shadow(0 0 32px #9ae6b91a); }
    .reel-hero-copy { max-width: 310px; text-wrap: balance; }
    .reel-start-cue.reel-start-cue { background: transparent; border: 0; color: #b1cabb; font-size: .8rem; padding: 12px 20px; }
    .reel-start-cue > .kobweb-row { gap: 10px; }
    .reel-touch-label { display: none; }
    @media (pointer: coarse) { .reel-touch-label { display: inline; } .reel-scroll-label { display: none; } }
    @media (hover: hover) and (pointer: fine) { .reel-swipe-hint { display: none; } }
    .reel-install { width: 100%; max-width: 320px; }
    .reel-install-instructions { padding: 14px; border: 1px solid #ffffff12; border-radius: 14px; background: #122219; text-align: left; }
    .reel-install-instructions span { color: #adc6b7; }
    .reel-player-choice { box-sizing: border-box; padding: calc(90px + env(safe-area-inset-top)) 22px max(28px, env(safe-area-inset-bottom)); }
    .reel-player-choice h2 { margin: 0; font-size: 1.8rem; letter-spacing: -.045em; }
    .player-choice-card.player-choice-card { box-shadow: 0 3px 16px #0000000b; }
    .player-choice-card.player-choice-card:hover { background: #1a3024; border-color: #638c73; }
    .player-portrait { background: radial-gradient(ellipse, #28473566, transparent 70%); border-radius: 28px; }
    .reel-player-intro h2 { text-wrap: balance; letter-spacing: -.045em; }
    .player-more-details { padding: 14px; border: 1px solid #ffffff0e; background: #102017; border-radius: 16px; box-sizing: border-box; }
    .shot-layout-overlay { box-sizing: border-box; padding-bottom: calc(52px + env(safe-area-inset-bottom)); }
    .shot-heading { text-align: center; }
    .shot-heading p { text-wrap: balance; }
    .coordinate-confirm { box-sizing: border-box; }
    .reel-fullscreen .coordinate-confirm { padding-bottom: max(16px, env(safe-area-inset-bottom)); }
    .coordinate-confirm > span { color: #9eb6a7; font-size: .76rem; text-align: center; line-height: 1.5; max-width: 300px; }
    .reel-utility-bar { position: absolute; bottom: 0; left: 16px; right: 16px; min-height: calc(50px + env(safe-area-inset-bottom));
      box-sizing: border-box; padding-bottom: env(safe-area-inset-bottom); display: flex; align-items: center; justify-content: space-between;
      z-index: 5; border-top: 1px solid #ffffff0e; gap: 8px; }
    .reel-score { color: #b7c9bd; font-size: .72rem; font-variant-numeric: tabular-nums; white-space: nowrap; }
    .reel-score strong { color: #eff7f1; font-weight: 700; }
    .reel-utility-button.reel-utility-button { min-height: 36px; padding: 6px 10px; border: 0; border-radius: 10px;
      background: transparent; color: #aac0b2; font-size: .72rem; display: inline-flex; align-items: center; gap: 6px; }
    .reel-utility-button.reel-utility-button:hover { background: #ffffff09; }
    .reel-video-frame { border-radius: 0; box-sizing: border-box; }
    .shot-player-avatar { width: 38px; height: 38px; flex-shrink: 0; border-radius: 50%; overflow: hidden; background: #203b2c; color: #ade7cb; }
    .shot-player-avatar img { width: 100%; height: 100%; object-fit: cover; }
    .reel-source-link { display: inline-flex; align-items: center; justify-content: center; gap: 7px; text-decoration: none; }
    .reel-source-link:hover { text-decoration: underline; }
    .reel-share-button.reel-share-button { flex: 1; min-width: 0; padding: 12px 8px; font-size: .72rem; }
    .reel-share-button > .kobweb-row { gap: 6px; }
    .reel-rack { background: radial-gradient(ellipse at center, #20412d44, transparent 65%); }
    .reel-rack h2 { margin: 0; letter-spacing: -.055em; font-size: 2.7rem; }
    .daily-challenge-page { background: transparent; padding-top: calc(96px + env(safe-area-inset-top)); gap: 18px; }
    .daily-challenge-page h2 { font-size: 1.85rem; letter-spacing: -.045em; font-weight: 750; }
    .daily-challenge-page > .daily-back { top: calc(66px + env(safe-area-inset-top)); left: 16px; width: 34px; height: 30px; }
    .daily-choice { padding-top: calc(90px + env(safe-area-inset-top)); padding-bottom: 36px; }
    .daily-brand { width: 64px; height: 64px; }
    .daily-options { gap: 12px; margin-top: 12px; }
    .daily-option { border-radius: 20px; padding: 20px; min-height: 88px; border-color: #536c5c; background: linear-gradient(135deg,#193023,#102017); box-shadow: 0 8px 25px #0002; }
    .daily-option-primary { border-color: #536c5c; background: linear-gradient(135deg,#193023,#102017); }
    .daily-option-icon { display: flex; align-items: center; justify-content: center; width: 42px; height: 42px; border-radius: 14px; background: #ade7cb0c; }
    .daily-option-label { font-size: 1rem; letter-spacing: -.02em; }
    .daily-option-detail { font-size: .76rem; line-height: 1.45; color: #a0b8a9; letter-spacing: 0; }
    .daily-alias-field { gap: 8px; }
    .daily-alias-field input { height: 50px; border-radius: 14px; border-color: #3b5344; background: #102017; padding: 12px 15px; }
    .daily-command { border-radius: 14px; min-height: 46px; background: #b0e6ca; border-color: #b0e6ca; color: #0b2618; }
    .daily-header { top: calc(64px + env(safe-area-inset-top)); height: 28px; justify-content: center; color: #afc8b7; letter-spacing: .04em; font-size: .69rem; }
    .daily-header .daily-back { width: 28px; height: 26px; }
    .daily-results { padding-top: calc(104px + env(safe-area-inset-top)); gap: 16px; }
    .daily-result-icon { background: #e9ca7e0d; padding: 16px; border-radius: 22px; border: 1px solid #e9ca7e22; box-sizing: content-box; }
    .daily-metrics { background: linear-gradient(135deg,#213c2b,#112219); border: 1px solid #ade7cb22; border-radius: 22px; padding: 22px 12px; gap: 10px; }
    .daily-metrics > * + * { border-left: 1px solid #ffffff12; padding-left: 22px; }
    .daily-metric-value { font-size: 2.1rem; letter-spacing: -.055em; font-weight: 750; }
    .daily-standings { background: #102017; border: 1px solid #ffffff0e; padding: 16px; border-radius: 20px; box-sizing: border-box; }
    .daily-breakdown th { text-transform: uppercase; font-size: .6rem; font-weight: 700; letter-spacing: .08em; color: #86a28f; }
    .daily-breakdown th, .daily-breakdown td { padding: 12px 6px; border-color: #ffffff0b; }
    .daily-breakdown tr:last-child td { border-bottom: 0; }
    .daily-shot-outcome { display: inline-flex; align-items: center; gap: 7px; font-size: .78rem; font-weight: 650; }
    .daily-own-result td:first-child { border-radius: 9px 0 0 9px; }
    .daily-own-result td:last-child { border-radius: 0 9px 9px 0; }
    .desktop-reel-navigation button { transition: transform .16s, border-color .16s; }
    .desktop-reel-navigation button:hover:not(:disabled) { transform: translateY(-2px); }
    .desktop-reel-navigation button:disabled { opacity: .25; }
    @media(max-height: 700px) {
      .eight-ball-reel-intro { gap: 14px; padding: max(22px,env(safe-area-inset-top)) 22px max(22px,env(safe-area-inset-bottom)); }
      .eight-ball-reel-logo { width: 72px; height: 72px; }
      .reel-hero-title { font-size: 2.2rem; }
      .eight-ball-reel-intro:has([aria-expanded=true]) .eight-ball-reel-brand { display: none; }
      .daily-choice { justify-content: safe center; }
      .daily-options { margin-top: 0; }
      .daily-option { min-height: 84px; padding: 15px; }
      .daily-results { padding-top: calc(102px + env(safe-area-inset-top)); }
    }
    @media(max-height: 650px) {
      .reel-player-intro:has([aria-expanded=true]) .player-portrait { display: none; }
    }
    @media(prefers-reduced-motion: reduce) { .eight-ball-app button { transition: none; } .reel-swipe-hint { animation: none !important; } }
""".trimIndent()
