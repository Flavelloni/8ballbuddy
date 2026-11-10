package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.css.px
import org.w3c.dom.events.Event

private var installPrompt: dynamic = null
private var appInstalled by mutableStateOf(false)

// Deliberately no service worker: installation is an enhancement, not an offline
// cache that might serve stale catalogs or download the user's entire video library.
internal fun prepareAppInstallation(): () -> Unit {
    for ((rel,path) in listOf("manifest" to "/manifest.webmanifest","apple-touch-icon" to "/images/app-icon-192.png")) {
        if (document.querySelector("link[rel='$rel']") == null) {
            val link=document.createElement("link")
            link.setAttribute("rel",rel);link.setAttribute("href",BasePath.prependTo(path))
            document.head?.appendChild(link)
        }
    }
    if(document.querySelector("meta[name='theme-color']")==null) {
        val meta=document.createElement("meta");meta.setAttribute("name","theme-color");meta.setAttribute("content","#08130f")
        document.head?.appendChild(meta)
    }
    appInstalled=window.matchMedia("(display-mode: standalone)").matches || window.navigator.asDynamic().standalone == true
    val before: (Event)->Unit={ event -> event.preventDefault();installPrompt=event.asDynamic() }
    val installed: (Event)->Unit={ appInstalled=true;installPrompt=null }
    window.addEventListener("beforeinstallprompt",before)
    window.addEventListener("appinstalled",installed)
    return {window.removeEventListener("beforeinstallprompt",before);window.removeEventListener("appinstalled",installed)}
}

@Composable
internal fun InstallAppButton() {
    var instructions by remember { mutableStateOf(false) }
    if(appInstalled) return
    Column(Modifier.gap(8.px).styleModifier {property("max-width","320px")},horizontalAlignment=Alignment.CenterHorizontally) {
        Button(onClick={
            val prompt=installPrompt
            if(prompt!=null) {
                try {
                    prompt.prompt()
                    prompt.userChoice.then({ choice: dynamic ->
                        if(choice.outcome=="accepted") appInstalled=true
                        installPrompt=null
                    },{ _: dynamic -> instructions=true })
                } catch(_: Throwable) { instructions=true }
            } else instructions=!instructions
        },modifier=Modifier.borderRadius(999.px).attrsModifier {
            attr("aria-label","Install 8-Ball Buddy");attr("aria-expanded",instructions.toString())
        }) { SpanText("Install app ↗") }
        if(instructions) {
            val apple=js("/iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)") as Boolean
            SpanText(if(apple) "On iPhone or iPad: open in Safari, tap Share, then Add to Home Screen and Open as Web App (if offered)."
                else "Use your browser menu → Install app or Add to Home screen. On desktop, look for the install icon in the address bar. If unavailable, keep using the website normally.",
                Modifier.styleModifier {property("font-size",".78rem");property("line-height","1.45");property("color","#adcabc")}
                    .attrsModifier {attr("role","status")})
            SpanText("Free · No app store needed · Internet required",Modifier.styleModifier {property("font-size",".7rem");property("color","#88a597")})
        }
    }
}
