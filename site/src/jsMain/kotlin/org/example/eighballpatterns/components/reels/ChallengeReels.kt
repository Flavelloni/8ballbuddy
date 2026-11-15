package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.silk.components.icons.fa.FaArrowLeft
import com.varabyte.kobweb.silk.components.icons.fa.FaArrowRight
import com.varabyte.kobweb.silk.components.icons.fa.FaRotateRight
import com.varabyte.kobweb.silk.components.icons.fa.FaPen
import com.varabyte.kobweb.silk.components.icons.fa.FaCalendarDay
import com.varabyte.kobweb.silk.components.icons.fa.FaAnglesUp
import com.varabyte.kobweb.silk.components.forms.Button as SilkButton
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.coroutines.delay
import org.example.eighballpatterns.browser.hasAutomaticChallengeNameHint
import org.example.eighballpatterns.browser.markChallengeNameHintSeen
import org.example.eighballpatterns.model.DailyChallengeAttempt
import org.example.eighballpatterns.model.DailyLeaderboard
import org.example.eighballpatterns.model.formatChallengeTime
import org.example.eighballpatterns.model.hasValidPlayerAliasLength
import org.example.eighballpatterns.model.normalizePlayerAlias
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Form
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLElement


@Composable
internal fun EightBallChallengeHeader(shotNumber: Int, total: Int, onLeave: () -> Unit) {
    Row(
        Modifier.attrsModifier { classes("daily-header") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(attrs = {
            classes("daily-back")
            attr("aria-label", "Leave daily challenge")
            title("Leave daily challenge")
            onClick { onLeave() }
        }) { FaArrowLeft() }
        SpanText("Daily challenge · $shotNumber/$total")
    }
}

@Composable
internal fun EightBallChallengeNext(lastShot: Boolean, onNext: () -> Unit) {
    Button(attrs = {
        classes("daily-command")
        onClick { onNext() }
    }) {
        Text(if (lastShot) "See results" else "Next shot")
        FaArrowRight()
    }
}

@Composable
internal fun EightBallChallengeResults(
    attempt: DailyChallengeAttempt,
    onBrowse: () -> Unit,
    leaderboard: DailyLeaderboard?,
    playerUid: String?,
    leaderboardBusy: Boolean,
    leaderboardError: String?,
    onRefresh: () -> Unit,
    allTimeLeaderboard: DailyLeaderboard?,
    allTimeBusy: Boolean,
    allTimeError: String?,
    onAllTimeRefresh: () -> Unit,
    alias: String,
    renameBusy: Boolean,
    renameError: String?,
    onRename: (String, () -> Unit) -> Unit,
    onEditingChanged: (Boolean) -> Unit = {},
) {
    var allTime by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var draft by remember(alias) { mutableStateOf(alias) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var submittedName by remember { mutableStateOf<String?>(null) }
    var nameSaved by remember { mutableStateOf(false) }
    var showNameHint by remember(playerUid) { mutableStateOf(hasAutomaticChallengeNameHint(playerUid)) }
    var nameInput by remember { mutableStateOf<HTMLInputElement?>(null) }
    var changeNameControl by remember { mutableStateOf<HTMLElement?>(null) }
    var restoreNameFocus by remember { mutableStateOf(false) }
    val visibleError = validationError ?: renameError?.takeIf { submittedName == normalizePlayerAlias(draft) }
    DisposableEffect(editing) {
        onEditingChanged(editing)
        onDispose { onEditingChanged(false) }
    }
    DisposableEffect(playerUid) {
        if (showNameHint) playerUid?.let { markChallengeNameHintSeen(it) }
        onDispose { }
    }
    LaunchedEffect(editing, nameInput) {
        if (editing) nameInput?.let { it.focus(); it.select() }
    }
    LaunchedEffect(editing, changeNameControl) {
        if (!editing && restoreNameFocus) changeNameControl?.let { it.focus(); restoreNameFocus = false }
    }
    LaunchedEffect(nameSaved) { if (nameSaved) { delay(3500); nameSaved = false } }
    Column(
        Modifier.attrsModifier {
            classes("daily-challenge-page", "daily-results", "daily-compact-results")
            if (editing) classes("daily-results-editing")
            attr("aria-label", "Challenge results")
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(Modifier.attrsModifier { classes("daily-results-content") }) {
        Row(Modifier.attrsModifier { classes("daily-result-hero") }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.attrsModifier { classes("daily-result-title") }) {
                SpanText(attempt.challenge.date, Modifier.attrsModifier { classes("daily-muted") })
                H2 { Text("Daily complete") }
            }
            Column(Modifier.attrsModifier { classes("daily-result-summary") }, horizontalAlignment = Alignment.End) {
                SpanText("${attempt.correctCount}/${attempt.answers.size}", Modifier.attrsModifier { classes("daily-result-score") })
                SpanText("correct · ${formatChallengeTime(attempt.totalElapsedMs)}")
            }
        }
        Column(Modifier.attrsModifier { classes("daily-player-identity"); attr("aria-label", "Your leaderboard name") }) {
          if (editing) {
            // Native form/label semantics preserve Enter-to-save and an explicit input association.
            Form(attrs = {
                classes("daily-rename-form")
                addEventListener("submit") { event ->
                    event.preventDefault()
                    if (!renameBusy) {
                        val chosen = normalizePlayerAlias(draft)
                        if (!hasValidPlayerAliasLength(chosen)) validationError = "Use 3 to 20 characters."
                        else {
                            validationError = null
                            submittedName = chosen
                            onRename(chosen) { editing = false; nameSaved = true; showNameHint = false; restoreNameFocus = true }
                        }
                    }
                }
            }) {
                Label(attrs = { attr("for", "daily-challenger-name"); classes("daily-identity-label") }) { Text("Your leaderboard name") }
                Input(InputType.Text, attrs = {
                    id("daily-challenger-name")
                    attr("aria-label", "Your challenger name")
                    attr("aria-describedby", "daily-name-help")
                    attr("aria-invalid", (visibleError != null).toString())
                    attr("maxlength", "20"); attr("autocomplete", "nickname")
                    attr("autocapitalize", "words"); attr("spellcheck", "false")
                    value(draft)
                    if (renameBusy) attr("disabled", "")
                    onInput { draft = it.value; validationError = null }
                    onKeyDown { if (it.key == "Escape" && !renameBusy) { it.preventDefault(); editing = false; restoreNameFocus = true } }
                    ref { input -> nameInput = input; onDispose { nameInput = null } }
                })
                Row(Modifier.attrsModifier { classes("daily-rename-actions") }) {
                    Button(attrs = { classes("daily-save-name"); attr("type", "submit"); attr("aria-label", "Save name"); if(renameBusy) attr("disabled", "") }) { Text(if(renameBusy) "Saving…" else "Save name") }
                    Button(attrs = { attr("type", "button"); if(renameBusy) attr("disabled", ""); onClick { editing = false; validationError = null; restoreNameFocus = true } }) { Text("Cancel") }
                }
                P(attrs = { id("daily-name-help"); classes("daily-name-help"); if (visibleError != null) attr("role", "alert") }) {
                    Text(visibleError ?: "3–20 characters · letters, numbers, spaces, _ or -")
                }
            }
          } else {
            Row(Modifier.attrsModifier { classes("daily-result-name") }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.attrsModifier { classes("daily-identity-copy") }) {
                    SpanText("Your leaderboard name", Modifier.attrsModifier { classes("daily-identity-label") })
                    SpanText(alias, Modifier.attrsModifier { classes("daily-player-name") })
                }
                SilkButton(onClick = { draft = alias; editing = true; validationError = null; submittedName = null; nameSaved = false },
                    modifier = Modifier.attrsModifier {
                        classes("daily-change-name"); attr("aria-label", "Change name")
                        ref { control -> changeNameControl = control as HTMLElement; onDispose { changeNameControl = null } }
                    }) {
                    Row(Modifier.attrsModifier { classes("daily-change-name-content") }, verticalAlignment = Alignment.CenterVertically) {
                        FaPen(); SpanText("Change name")
                    }
                }
            }
            if (showNameHint) P(attrs = { classes("daily-name-hint") }) { Text("We picked a name for you. Make it yours.") }
            if (nameSaved) P(attrs = { classes("daily-name-saved"); attr("role", "status") }) { Text("Name updated") }
          }
        }
        Column(Modifier.attrsModifier { classes("daily-standings-block") }) {
        Row(Modifier.attrsModifier { classes("daily-leaderboard-tabs"); attr("role", "group"); attr("aria-label", "Standings period") }) {
            for ((value, label) in listOf(false to "Today", true to "All time")) {
                Button(attrs = {
                    attr("aria-pressed", (allTime == value).toString())
                    onClick { allTime = value }
                }) { Text(label) }
            }
        }
        CompactChallengeLeaderboard(
            if(allTime) allTimeLeaderboard else leaderboard, playerUid,
            if(allTime) allTimeBusy else leaderboardBusy,
            if(allTime) allTimeError else leaderboardError,
            allTime, if(allTime) onAllTimeRefresh else onRefresh,
        )
        }
        Row(Modifier.attrsModifier { classes("daily-tomorrow"); attr("aria-label", "Next daily challenge") }, verticalAlignment = Alignment.CenterVertically) {
            FaCalendarDay()
            Column {
                SpanText("New challenge tomorrow", Modifier.attrsModifier { classes("daily-tomorrow-title") })
                SpanText("Come back at 00:00 UTC", Modifier.attrsModifier { classes("daily-tomorrow-time") })
            }
        }
        Column(Modifier.attrsModifier { classes("daily-practice-actions") }, horizontalAlignment = Alignment.CenterHorizontally) {
            SilkButton(onClick = { onBrowse() }, enabled = !editing, modifier = Modifier.attrsModifier {
                classes("daily-more-shots", "reel-primary-button"); attr("aria-label", "See more shots")
            }) { Row(Modifier.attrsModifier { classes("daily-more-shots-content") }, verticalAlignment = Alignment.CenterVertically) { SpanText("See more shots"); FaArrowRight() } }
            Row(Modifier.attrsModifier { classes("daily-results-swipe") }, verticalAlignment = Alignment.CenterVertically) { FaAnglesUp(); SpanText("or swipe up to explore") }
        }
      }
    }
}

@Composable
internal fun EightBallChallengeStartError(message: String, onRetry: () -> Unit, onBrowse: () -> Unit) {
    Column(Modifier.attrsModifier { classes("daily-challenge-page", "daily-choice") }, horizontalAlignment=Alignment.CenterHorizontally) {
        H2 { Text("Could not start the challenge") }
        P(attrs={attr("role","alert")}) { Text(message) }
        Button(attrs={classes("daily-command");onClick {onRetry()}}) { Text("Try again") }
        Button(attrs={classes("daily-command");onClick {onBrowse()}}) { Text("Browse practice shots") }
    }
}

@Composable
internal fun EightBallChallengeRequestStatus(busy: Boolean, error: String?, onRetry: () -> Unit,
    loadingLabel: String = "Connecting…") {
    Row(Modifier.attrsModifier { classes("daily-request-status"); attr("role", if (error != null) "alert" else "status") },
        verticalAlignment = Alignment.CenterVertically) {
        if (busy) PoolLoading(loadingLabel) else SpanText(error.orEmpty())
        if (!busy && error != null) {
            Button(attrs = {
                classes("daily-back")
                attr("aria-label", "Retry request")
                title("Retry request")
                onClick { onRetry() }
            }) { FaRotateRight() }
        }
    }
}

internal val dailyChallengeStyles = """
    .daily-results.daily-compact-results { padding: max(72px, calc(68px + env(safe-area-inset-top))) 18px max(12px, env(safe-area-inset-bottom)); gap: 0; justify-content: safe center; overflow-y: auto; }
    .daily-results-content { width: 100%; max-width: 440px; gap: 12px; flex-shrink: 0; }
    .daily-result-hero { width: 100%; justify-content: space-between; gap: 12px; }
    .daily-result-title { gap: 3px; text-align: left; }
    .daily-result-title .daily-muted { font-size: .68rem; }
    .daily-results.daily-compact-results h2 { font-size: 1.35rem; margin: 0; }
    .daily-result-summary { gap: 3px; font-size: .72rem; white-space: nowrap; }
    .daily-result-score { font-size: 2.1rem; line-height: 1; font-weight: 800; color: #ade7cb; }
    .daily-player-identity { width: 100%; box-sizing: border-box; padding: 12px; gap: 6px; text-align: left; background: #13241c; border: 1px solid #ade7cb26; border-radius: 14px; }
    .daily-result-name { width: 100%; gap: 10px; justify-content: space-between; }
    .daily-identity-copy { min-width: 0; gap: 3px; }
    .daily-identity-label { font-size: .65rem; color: #a1baac; line-height: 1.3; }
    .daily-player-name { font-size: 1rem; font-weight: 700; line-height: 1.25; overflow-wrap: anywhere; }
    .daily-change-name.daily-change-name { min-height: 44px; flex-shrink: 0; padding: 0 10px; border-radius: 10px; border: 1px solid #ade7cb44; background: #ade7cb09; color: #d1efde; font-size: .72rem; font-weight: 650; }
    .daily-change-name-content { gap: 7px; }
    .daily-change-name:hover { background: #ade7cb16; }
    .daily-compact-results .daily-name-hint, .daily-compact-results .daily-name-saved { font-size: .7rem; line-height: 1.35; color: #b5cabe; }
    .daily-compact-results .daily-name-saved { color: #ade7cb; }
    .daily-rename-form { display: flex; flex-direction: column; width: 100%; gap: 8px; }
    .daily-rename-form input { box-sizing: border-box; width: 100%; min-width: 0; height: 44px; padding: 9px 11px; border-radius: 9px; border: 1px solid #638c73; color: #edf6f0; background: #091710; font-size: 16px; }
    .daily-rename-form input:focus-visible { outline: 2px solid #ade7cb; outline-offset: 2px; }
    .daily-rename-form input[aria-invalid=true] { border-color: #f4a4a4; }
    .daily-rename-actions { gap: 8px; }
    .daily-rename-actions button { min-height: 44px; padding: 9px 14px; border-radius: 9px; font: inherit; font-size: .78rem; cursor: pointer; border: 1px solid #ade7cb33; color: #c9dfd2; background: transparent; }
    .daily-rename-actions .daily-save-name { background: #ade7cb; color: #102219; border-color: #ade7cb; font-weight: 700; }
    .daily-rename-actions button:disabled { opacity: .5; cursor: wait; }
    .daily-compact-results .daily-name-help { font-size: .65rem; line-height: 1.35; color: #a1baac; }
    .daily-rename-form:has(input[aria-invalid=true]) .daily-name-help { color: #f4a4a4; }
    .daily-standings-block { width: 100%; gap: 4px; }
    .daily-leaderboard-tabs { width: 100%; gap: 6px; padding: 3px; box-sizing: border-box; border-radius: 11px; background: #ffffff05; border: 1px solid #ffffff10; }
    .daily-leaderboard-tabs button { flex: 1; min-height: 38px; color: #a8c1b2; background: transparent; border: 0; border-radius: 8px; padding: 6px 9px; font: inherit; font-size: .75rem; cursor: pointer; }
    .daily-leaderboard-tabs button[aria-pressed=true] { background: #ade7cb; color: #102219; }
    .daily-compact-standings { width: 100%; min-height: 138px; gap: 2px; }
    .daily-compact-standings-heading { width: 100%; justify-content: space-between; font-size: .73rem; }
    .daily-compact-standings-heading .daily-back { min-height: 28px; height: 28px; }
    .daily-compact-standings .daily-breakdown td { padding: 6px 4px; font-size: .76rem; overflow-wrap: anywhere; }
    .daily-compact-standings .daily-leaderboard td:first-child { width: 32px; }
    .daily-compact-standings .daily-leaderboard td:last-child { width: 46px; }
    .daily-compact-results .daily-tomorrow { width: 100%; box-sizing: border-box; padding: 12px; gap: 12px; border-radius: 12px; border: 1px solid #e6c97b55; background: linear-gradient(110deg, #e6c97b17, #e6c97b06); color: #eedcae; text-align: left; }
    .daily-tomorrow > .fas { font-size: 1.2rem; }
    .daily-tomorrow-title { font-size: .88rem; line-height: 1.3; font-weight: 750; }
    .daily-tomorrow-time { font-size: .7rem; line-height: 1.5; color: #c2bda9; }
    .daily-practice-actions { width: 100%; gap: 6px; }
    .daily-more-shots.daily-more-shots { width: 100%; min-height: 46px; border-radius: 12px; padding: 10px 16px; }
    .daily-more-shots-content { gap: 9px; }
    .daily-results-swipe { gap: 6px; font-size: .66rem; color: #9ab2a3; }
    .daily-results-editing.daily-results-editing { justify-content: flex-start; }
    @media (max-height: 650px) {
      .daily-results.daily-compact-results { padding-top: calc(66px + env(safe-area-inset-top)); padding-bottom: max(8px, env(safe-area-inset-bottom)); }
      .daily-results-content { gap: 6px; }
      .daily-results.daily-compact-results h2 { font-size: 1.15rem; }
      .daily-result-score { font-size: 1.8rem; }
      .daily-player-identity { padding: 8px 10px; gap: 3px; }
      .daily-compact-results .daily-name-hint { font-size: .65rem; }
      .daily-leaderboard-tabs button { min-height: 34px; }
      .daily-compact-results .daily-compact-standings { min-height: 0; }
      .daily-compact-standings .daily-breakdown td { padding-top: 3px; padding-bottom: 3px; font-size: .72rem; line-height: 1.25; }
      .daily-compact-results .daily-tomorrow { padding: 8px 10px; gap: 10px; }
      .daily-tomorrow-title { font-size: .8rem; }
      .daily-tomorrow-time { font-size: .66rem; }
      .daily-more-shots.daily-more-shots { min-height: 44px; padding-top: 8px; padding-bottom: 8px; }
      .daily-practice-actions { gap: 3px; }
    }
    .daily-challenge-page {
      width: 100%; height: 100%; box-sizing: border-box; position: relative;
      padding: max(48px, env(safe-area-inset-top)) 24px max(24px, env(safe-area-inset-bottom));
      overflow-y: auto; touch-action: pan-y; color: #f5faf7; gap: 16px; text-align: center;
    }
    .daily-challenge-page h2 { margin: 0; font-size: 1.8rem; line-height: 1.2; }
    .daily-challenge-page h3 { margin: 0; font-size: 1rem; }
    .daily-challenge-page p { margin: 0; font-size: .86rem; line-height: 1.4; }
    .daily-choice { justify-content: center; }
    .daily-brand { width: 80px; height: 80px; flex-shrink: 0; }
    .daily-options { width: 100%; gap: 12px; margin-top: 12px; }
    .daily-alias-form { width: 100%; display: flex; flex-direction: column; gap: 12px; margin: 0; }
    .daily-alias-field { width: 100%; gap: 6px; text-align: left; font-size: .82rem; color: #a9b5af; }
    .daily-alias-field input {
      width: 100%; min-width: 0; height: 44px; box-sizing: border-box; padding: 10px 12px;
      border: 1px solid #46534c; border-radius: 6px; background: #101514; color: #f5faf7;
      font: inherit; font-size: 1rem;
    }
    .daily-alias-field input:focus-visible { outline: 2px solid #94c9b5; outline-offset: 2px; }
    .daily-alias-field input:read-only { border-color: #26302c; color: #a9b5af; }
    .daily-option {
      width: 100%; min-height: 96px; display: flex; align-items: center; gap: 16px;
      padding: 18px; border: 1px solid #343b39; border-radius: 8px;
      background: #101514; color: #f5faf7; cursor: pointer; text-align: left; font: inherit;
    }
    .daily-option-primary { border-color: #94c9b5; background: #14251e; }
    .daily-option:disabled { opacity: .5; cursor: default; }
    .daily-option-icon { font-size: 1.3rem; color: #94c9b5; width: 26px; flex-shrink: 0; }
    .daily-option-label { gap: 6px; flex: 1; min-width: 0; font-size: 1rem; font-weight: 700; }
    .daily-option-label > span { overflow-wrap: anywhere; }
    .daily-option-detail { font-size: .78rem; color: #a9b5af; font-weight: 400; }
    .daily-muted { color: #a9b5af; font-size: .78rem; line-height: 1.4; }
    .daily-back {
      width: 40px; height: 40px; flex-shrink: 0; display: inline-flex; align-items: center;
      justify-content: center; border: 0; border-radius: 8px; background: transparent;
      color: #f5faf7; font-size: 1rem; cursor: pointer;
    }
    .daily-challenge-page > .daily-back { position: absolute; top: max(8px, env(safe-area-inset-top)); left: 12px; }
    .daily-header {
      position: absolute; top: calc(60px + env(safe-area-inset-top)); left: 12px; right: 12px;
      height: 40px; z-index: 7; gap: 8px; color: #94c9b5; font-size: .78rem; font-weight: 700;
    }
    .daily-command {
      min-height: 44px; padding: 10px 18px; display: inline-flex; align-items: center;
      justify-content: center; gap: 12px; border: 1px solid #94c9b5; border-radius: 8px;
      background: #94c9b5; color: #0b1713; font: inherit; font-size: .88rem; font-weight: 700;
      cursor: pointer; flex-shrink: 0;
    }
    .daily-option:hover, .daily-back:hover { background: #1b3027; }
    .daily-command:hover { background: #b2dfce; }
    .daily-option:focus-visible, .daily-command:focus-visible, .daily-back:focus-visible {
      outline: 2px solid white; outline-offset: 3px;
    }
    .daily-results { gap: 12px; }
    .daily-result-icon { font-size: 1.6rem; color: #e9ca7e; flex-shrink: 0; }
    .daily-metrics { width: 100%; justify-content: space-evenly; gap: 16px; padding: 12px 0; flex-shrink: 0; }
    .daily-metric-value { font-size: 1.9rem; font-weight: 700; font-variant-numeric: tabular-nums; line-height: 1.3; }
    .daily-breakdown { width: 100%; border-collapse: collapse; font-size: .82rem; font-variant-numeric: tabular-nums; }
    .daily-breakdown th { color: #a9b5af; font-weight: 500; }
    .daily-breakdown th, .daily-breakdown td { padding: 8px 4px; border-bottom: 1px solid #26302c; text-align: center; }
    .daily-breakdown td i { margin-left: 5px; font-size: .65rem; }
    .daily-correct { color: #94c9b5; }
    .daily-incorrect { color: #f4a4a4; }
    .daily-standings { width: 100%; gap: 6px; padding: 12px 0; }
    .daily-request-status { gap: 4px; max-width: 100%; font-size: .75rem; color: #e9ca7e; text-align: center; }
    .daily-request-status > span { overflow-wrap: anywhere; }
    .daily-back:disabled, .daily-command:disabled { opacity: .5; cursor: default; }
    .daily-standings-heading { width: 100%; justify-content: space-between; gap: 12px; text-align: left; }
    .daily-leaderboard { table-layout: fixed; }
    .daily-leaderboard th:first-child { width: 30px; }
    .daily-leaderboard th:nth-child(3) { width: 54px; }
    .daily-leaderboard th:last-child { width: 70px; }
    .daily-leaderboard td:nth-child(2), .daily-leaderboard th:nth-child(2) { text-align: left; overflow-wrap: anywhere; }
    .daily-leaderboard .daily-own-result { background: #172b23; }
    .daily-own-label { color: #94c9b5; font-size: .72rem; }
    @media (max-height: 650px) {
      .daily-challenge-page { gap: 10px; padding-left: 20px; padding-right: 20px; }
      .daily-brand { width: 56px; height: 56px; }
      .daily-options { margin-top: 4px; gap: 10px; }
      .daily-option { min-height: 80px; padding: 14px; }
    }
    @media (max-height: 440px) {
      .daily-choice { justify-content: flex-start; padding-top: 12px; }
      .daily-brand { width: 40px; height: 40px; }
      .daily-challenge-page h2 { font-size: 1.4rem; }
      .daily-option { min-height: 68px; padding: 10px 14px; }
    }
""".trimIndent()
