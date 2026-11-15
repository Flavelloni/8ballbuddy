package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
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
import com.varabyte.kobweb.silk.components.text.SpanText
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
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text


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
) {
    var allTime by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var draft by remember(alias) { mutableStateOf(alias) }
    var validationError by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.attrsModifier { classes("daily-challenge-page", "daily-results", "daily-compact-results"); attr("aria-label", "Challenge results") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpanText(attempt.challenge.date, Modifier.attrsModifier { classes("daily-muted") })
        H2 { Text("Daily complete") }
        Row(Modifier.attrsModifier { classes("daily-result-summary") }, verticalAlignment = Alignment.CenterVertically) {
            SpanText("${attempt.correctCount}/${attempt.answers.size}", Modifier.attrsModifier { classes("daily-result-score") })
            SpanText("correct · ${formatChallengeTime(attempt.totalElapsedMs)}")
        }
        if (editing) {
            Form(attrs = {
                classes("daily-rename-form")
                addEventListener("submit") { event ->
                    event.preventDefault()
                    if (!renameBusy) {
                        val chosen = normalizePlayerAlias(draft)
                        if (!hasValidPlayerAliasLength(chosen)) validationError = "Use 3 to 20 characters."
                        else { validationError = null; onRename(chosen) { editing = false } }
                    }
                }
            }) {
                Input(InputType.Text, attrs = {
                    attr("aria-label", "Your challenger name")
                    attr("maxlength", "20"); attr("autocomplete", "nickname")
                    value(draft)
                    if (renameBusy) attr("disabled", "")
                    onInput { draft = it.value; validationError = null }
                })
                Button(attrs = { attr("type", "submit"); attr("aria-label", "Save name"); if(renameBusy) attr("disabled", "") }) { Text(if(renameBusy) "Saving…" else "Save") }
                Button(attrs = { attr("type", "button"); if(renameBusy) attr("disabled", ""); onClick { editing = false } }) { Text("Cancel") }
            }
            (validationError ?: renameError)?.let { message ->
                P(attrs = { classes("daily-incorrect", "daily-rename-error"); attr("role", "alert") }) { Text(message) }
            }
        } else {
            Row(Modifier.attrsModifier {classes("daily-result-name")}, verticalAlignment = Alignment.CenterVertically) {
                SpanText(alias)
                Button(attrs = {
                    classes("daily-back"); attr("aria-label", "Rename")
                    onClick { draft = alias; editing = true; validationError = null }
                }) { Text("Rename") }
            }
        }
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
        P(attrs = { classes("daily-tomorrow") }) { Text("New challenge tomorrow · 00:00 UTC") }
        Button(attrs = {
            classes("daily-more-shots"); attr("aria-label", "See more shots")
            onClick { onBrowse() }
        }) { EightBallReelSwipeHint("See more shots!") }
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
    .daily-results.daily-compact-results { padding: max(68px, calc(64px + env(safe-area-inset-top))) 18px max(8px, env(safe-area-inset-bottom)); gap: 6px; justify-content: center; overflow: hidden; }
    .daily-results.daily-compact-results h2 { font-size: 1.35rem; margin: 0; }
    .daily-result-summary { gap: 12px; font-size: .8rem; }
    .daily-result-score { font-size: 2rem; font-weight: 800; color: #ade7cb; }
    .daily-result-name { gap: 8px; max-width: 100%; font-size: .85rem; }
    .daily-result-name .daily-back { font-size: .72rem; min-height: 30px; }
    .daily-rename-form { display: flex; width: 100%; gap: 5px; }
    .daily-rename-form input { min-width: 0; flex: 1; padding: 7px; border-radius: 6px; border: 1px solid #46534c; color: #edf6f0; background: #101514; font-size: 16px; }
    .daily-rename-form button, .daily-leaderboard-tabs button { color: #ade7cb; background: #14251e; border: 1px solid #46534c; border-radius: 6px; padding: 7px 9px; }
    .daily-leaderboard-tabs { gap: 8px; }
    .daily-leaderboard-tabs button[aria-pressed=true] { background: #ade7cb; color: #102219; }
    .daily-compact-standings { width: 100%; min-height: 138px; gap: 2px; }
    .daily-compact-standings-heading { width: 100%; justify-content: space-between; font-size: .73rem; }
    .daily-compact-standings-heading .daily-back { min-height: 28px; height: 28px; }
    .daily-compact-standings .daily-breakdown td { padding: 6px 4px; font-size: .76rem; overflow-wrap: anywhere; }
    .daily-compact-standings .daily-leaderboard td:first-child { width: 32px; }
    .daily-compact-standings .daily-leaderboard td:last-child { width: 46px; }
    .daily-compact-results .daily-tomorrow { font-size: .72rem; color: #a9b5af; }
    .daily-more-shots { padding: 0; border: 0; background: transparent; color: #ade7cb; }
    .daily-more-shots .reel-swipe-hint { display: flex; height: 52px; gap: 3px; }
    @media (max-height: 600px) {
      .daily-results.daily-compact-results { gap: 3px; }
      .daily-compact-results .daily-result-score { font-size: 1.6rem; }
      .daily-compact-results .daily-compact-standings { min-height: 120px; }
      .daily-compact-standings .daily-breakdown td { padding-top: 4px; padding-bottom: 4px; }
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
