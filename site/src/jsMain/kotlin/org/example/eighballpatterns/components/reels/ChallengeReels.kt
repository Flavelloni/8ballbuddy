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
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaArrowLeft
import com.varabyte.kobweb.silk.components.icons.fa.FaArrowRight
import com.varabyte.kobweb.silk.components.icons.fa.FaCheck
import com.varabyte.kobweb.silk.components.icons.fa.FaPlay
import com.varabyte.kobweb.silk.components.icons.fa.FaRotateRight
import com.varabyte.kobweb.silk.components.icons.fa.FaTrophy
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
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr
import org.jetbrains.compose.web.dom.Table

@Composable
internal fun EightBallModeChoice(
    onDailyChallenge: () -> Unit,
    onBrowse: () -> Unit,
) {
    Column(
        Modifier.attrsModifier { classes("daily-challenge-page", "daily-choice") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Img(src = BasePath.prependTo("/images/pool-ball-icon.svg"), alt = "8-Ball Buddy", attrs = {
            classes("daily-brand")
            attr("width", "80")
            attr("height", "80")
        })
        H2 { Text("Your next move") }
        Column(Modifier.attrsModifier { classes("daily-options") }) {
            Button(attrs = {
                classes("daily-option", "daily-option-primary")
                attr("aria-label", "Daily challenge")
                onClick { onDailyChallenge() }
            }) {
                FaTrophy(Modifier.attrsModifier { classes("daily-option-icon") })
                SpanText("Daily challenge", Modifier.attrsModifier { classes("daily-option-label") })
                FaArrowRight()
            }
            P(attrs = { classes("daily-muted"); style { property("align-self", "center") } }) { Text("or") }
            Button(attrs = {
                classes("daily-option")
                attr("aria-label", "Just browse")
                onClick { onBrowse() }
            }) {
                FaPlay(Modifier.attrsModifier { classes("daily-option-icon") })
                SpanText("Just browse", Modifier.attrsModifier { classes("daily-option-label") })
                FaArrowRight()
            }
        }
    }
}

@Composable
internal fun EightBallChallengeEntry(
    attempt: DailyChallengeAttempt,
    onDailyChallenge: (String) -> Unit,
    onBack: () -> Unit,
    alias: String?,
    aliasLocked: Boolean,
    busy: Boolean,
    error: String?,
) {
    var aliasDraft by remember(alias) { mutableStateOf(alias.orEmpty()) }
    var aliasError by remember(alias) { mutableStateOf<String?>(null) }
    val visibleError = aliasError ?: error
    val dailyLabel = when {
        attempt.complete && attempt.challenge.shotIds.isNotEmpty() -> "View today's results"
        aliasLocked || attempt.answers.isNotEmpty() || attempt.currentShotElapsedMs > 0 -> "Resume daily challenge"
        else -> "Start challenge"
    }
    Column(
        Modifier.attrsModifier { classes("daily-challenge-page", "daily-choice") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(attrs = {
            classes("daily-back")
            attr("aria-label", "Back to choices")
            title("Back to choices")
            onClick { onBack() }
        }) { FaArrowLeft() }
        Img(src = BasePath.prependTo("/images/pool-ball-icon.svg"), alt = "8-Ball Buddy", attrs = {
            classes("daily-brand")
            attr("width", "80")
            attr("height", "80")
        })
        H2 { Text("Daily challenge") }
        Column(Modifier.attrsModifier { classes("daily-options") }) {
            Form(attrs = {
                classes("daily-alias-form")
                addEventListener("submit") { event ->
                    event.preventDefault()
                    if (!busy && attempt.challenge.shotIds.isNotEmpty()) {
                        val chosenAlias = normalizePlayerAlias(aliasDraft)
                        if (aliasLocked || hasValidPlayerAliasLength(chosenAlias)) {
                            aliasError = null
                            onDailyChallenge(chosenAlias)
                        } else {
                            aliasError = "Your nickname must have 3 to 20 characters."
                        }
                    }
                }
            }) {
                Column(Modifier.attrsModifier { classes("daily-alias-field") }) {
                    Label(forId = "daily-player-alias") { Text("Nickname") }
                    Input(InputType.Text, attrs = {
                        id("daily-player-alias")
                        value(aliasDraft)
                        attr("required", "")
                        attr("minlength", "3")
                        attr("maxlength", "20")
                        attr("autocomplete", "nickname")
                        attr("autocapitalize", "none")
                        attr("spellcheck", "false")
                        if (busy) attr("disabled", "")
                        if (aliasLocked) {
                            attr("readonly", "")
                            title("Name for today's challenge")
                        }
                        onInput { aliasDraft = it.value; aliasError = null }
                    })
                }
                Button(attrs = {
                    classes("daily-option", "daily-option-primary")
                    attr("type", "submit")
                    attr("aria-label", dailyLabel)
                    if (busy || attempt.challenge.shotIds.isEmpty()) attr("disabled", "")
                    attr("aria-busy", busy.toString())
                }) {
                    FaTrophy(Modifier.attrsModifier { classes("daily-option-icon") })
                    Column(Modifier.attrsModifier { classes("daily-option-label") }) {
                        SpanText(if (busy) "Connecting..." else dailyLabel)
                        SpanText("${attempt.challenge.shotIds.size} shots · ${attempt.challenge.date}", Modifier.attrsModifier {
                            classes("daily-option-detail")
                        })
                    }
                    FaArrowRight()
                }
            }
        }
        if (visibleError != null) {
            P(attrs = { classes("daily-incorrect"); attr("role", "alert") }) { Text(visibleError) }
        }
        if (attempt.answers.isNotEmpty()) {
            P(attrs = { classes("daily-muted") }) {
                Text(if (attempt.complete) "${attempt.correctCount}/${attempt.answers.size} correct · ${formatChallengeTime(attempt.totalElapsedMs)}"
                else "${attempt.answers.size} of ${attempt.challenge.shotIds.size} answered")
            }
        }
    }
}

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
        attr("aria-label", if (lastShot) "See results" else "Next shot")
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
    onBack: () -> Unit,
    leaderboard: DailyLeaderboard?,
    playerUid: String?,
    leaderboardBusy: Boolean,
    leaderboardError: String?,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(
        Modifier.attrsModifier { classes("daily-challenge-page", "daily-results") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(attrs = {
            classes("daily-back")
            attr("aria-label", "Back to choices")
            title("Back to choices")
            onClick { onBack() }
        }) { FaArrowLeft() }
        FaTrophy(Modifier.attrsModifier { classes("daily-result-icon") })
        SpanText(attempt.challenge.date, Modifier.attrsModifier { classes("daily-muted") })
        H2 { Text("Daily complete") }
        Row(Modifier.attrsModifier { classes("daily-metrics") }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SpanText("${attempt.correctCount}/${attempt.answers.size}", Modifier.attrsModifier { classes("daily-metric-value") })
                SpanText("Correct", Modifier.attrsModifier { classes("daily-muted") })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SpanText(formatChallengeTime(attempt.totalElapsedMs), Modifier.attrsModifier { classes("daily-metric-value") })
                SpanText("Answer time", Modifier.attrsModifier { classes("daily-muted") })
            }
        }
        EightBallLeaderboard(leaderboard, playerUid, leaderboardBusy, leaderboardError, onRefresh, onLoadMore)
        H3 { Text("Your shots") }
        Table(attrs = { classes("daily-breakdown"); attr("aria-label", "Challenge answers") }) {
            Thead {
                Tr {
                    listOf("Shot", "Pick", "Answer", "Time").forEach { label ->
                        Th(attrs = { attr("scope", "col") }) { Text(label) }
                    }
                }
            }
            Tbody {
                attempt.answers.forEachIndexed { index, answer ->
                    Tr {
                        Td { Text((index + 1).toString()) }
                        Td(attrs = { classes(if (answer.correct) "daily-correct" else "daily-incorrect") }) {
                            Text(answer.selectedBall.toString())
                            if (answer.correct) FaCheck(Modifier.attrsModifier { attr("aria-label", "Correct") })
                        }
                        Td { Text(answer.correctBall.toString()) }
                        Td { Text(formatChallengeTime(answer.elapsedMs)) }
                    }
                }
            }
        }
        Button(attrs = { classes("daily-command"); attr("aria-label", "Keep browsing"); onClick { onBrowse() } }) {
            Text("Keep browsing")
            FaArrowRight()
        }
    }
}

@Composable
internal fun EightBallChallengeRequestStatus(busy: Boolean, error: String?, onRetry: () -> Unit) {
    Row(Modifier.attrsModifier { classes("daily-request-status"); attr("role", if (error != null) "alert" else "status") },
        verticalAlignment = Alignment.CenterVertically) {
        SpanText(if (busy) "Connecting..." else error.orEmpty())
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
      position: absolute; top: max(4px, env(safe-area-inset-top)); left: 12px; right: 12px;
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
