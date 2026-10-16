package org.example.eighballpatterns.data

import kotlin.js.Promise
import kotlinx.browser.window

@JsModule("firebase/app")
@JsNonModule
private external object FirebaseAppSdk {
    fun initializeApp(config: dynamic): dynamic
    fun getApps(): Array<dynamic>
    fun getApp(): dynamic
}

@JsModule("firebase/auth")
@JsNonModule
private external object FirebaseAuthSdk {
    fun getAuth(app: dynamic): dynamic
    fun onAuthStateChanged(auth: dynamic, listener: (dynamic) -> Unit): () -> Unit
    fun signInAnonymously(auth: dynamic): Promise<dynamic>
    fun connectAuthEmulator(auth: dynamic, url: String, options: dynamic)
}

@JsModule("firebase/functions")
@JsNonModule
private external object FirebaseFunctionsSdk {
    fun getFunctions(app: dynamic, region: String): dynamic
    fun httpsCallable(functions: dynamic, name: String): (dynamic) -> Promise<dynamic>
    fun connectFunctionsEmulator(functions: dynamic, host: String, port: Int)
}

internal data class ChallengePlayer(val uid: String)

private fun firebaseConfiguration(useEmulators: Boolean): dynamic {
    val config = js("({})")
    config.apiKey = "AIzaSyBa5rHcNQCUbml4rg4krVLy_SkhFBuyP4A"
    config.authDomain = "ballbuddy-97095.firebaseapp.com"
    config.projectId = if (useEmulators) "demo-ballbuddy" else "ballbuddy-97095"
    config.storageBucket = "ballbuddy-97095.firebasestorage.app"
    config.messagingSenderId = "1034086450839"
    config.appId = "1:1034086450839:web:4663fe83af184af6cd8bd2"
    config.measurementId = "G-H8DFNTTMC3"
    return config
}

internal object FirebaseClient {
    private val useEmulators = try {
        window.location.hostname in listOf("localhost", "127.0.0.1") &&
            window.localStorage.getItem("ballbuddy-firebase-emulators") == "true"
    } catch (_: Throwable) { false }
    private val config = firebaseConfiguration(useEmulators)
    private val app = if (FirebaseAppSdk.getApps().isEmpty()) FirebaseAppSdk.initializeApp(config) else FirebaseAppSdk.getApp()
    private val auth = FirebaseAuthSdk.getAuth(app)
    private val functions = FirebaseFunctionsSdk.getFunctions(app, "europe-west1")

    init {
        if (useEmulators) {
            FirebaseAuthSdk.connectAuthEmulator(auth, "http://127.0.0.1:9099", js("({disableWarnings: true})"))
            FirebaseFunctionsSdk.connectFunctionsEmulator(functions, "127.0.0.1", 5001)
        }
    }

    fun observePlayer(listener: (ChallengePlayer?) -> Unit): () -> Unit =
        FirebaseAuthSdk.onAuthStateChanged(auth) { user ->
            listener(if (user == null) null else ChallengePlayer(user.uid as String))
        }

    fun signIn(): Promise<dynamic> = FirebaseAuthSdk.signInAnonymously(auth)

    fun call(name: String, input: dynamic = js("({})")): Promise<dynamic> =
        FirebaseFunctionsSdk.httpsCallable(functions, name)(input).then<dynamic> { result: dynamic -> result.data }
}

internal fun firebaseErrorMessage(error: Throwable): String = when (error.asDynamic().code as? String) {
    "auth/operation-not-allowed", "auth/configuration-not-found" -> "The daily challenge isn't available yet. Please try browsing."
    "auth/too-many-requests" -> "Too many attempts to connect. Please try again later."
    "functions/unavailable", "functions/not-found", "functions/internal", "auth/network-request-failed" ->
        "Could not reach the daily challenge. Please try again."
    else -> error.message ?: "Could not load the daily challenge. Please try again."
}
