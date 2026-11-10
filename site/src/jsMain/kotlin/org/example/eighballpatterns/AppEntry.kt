package org.example.eighballpatterns

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.example.eighballpatterns.components.reels.prepareAppInstallation
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.theme.colors.ColorMode

@InitSilk
fun initColorMode(ctx: InitSilkContext) {
    ctx.config.initialColorMode = ColorMode.DARK
}

@App
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    DisposableEffect(Unit) {
        val dispose=prepareAppInstallation()
        onDispose { dispose() }
    }
    SilkApp {
        content()
    }
}
