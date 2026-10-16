import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import groovy.json.JsonOutput
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.application)
}

group = "org.example.eighballpatterns"
version = "1.0-SNAPSHOT"

// Firebase 13's npm packages require Node 24.12 or newer.
rootProject.allprojects {
    plugins.withType<NodeJsPlugin> {
        project.extensions.configure<NodeJsEnvSpec> { version.set("24.13.0") }
    }
}

val generatedShotResources = layout.buildDirectory.dir("generated/shotResources")

val generateEightBallManifest = tasks.register("generateEightBallManifest") {
    val shotsDir = layout.projectDirectory.dir("src/jsMain/resources/public/8ball")
    val manifestFile = generatedShotResources.map { it.file("public/8ball/index.json") }

    inputs.dir(shotsDir)
    outputs.file(manifestFile)

    doLast {
        val root = shotsDir.asFile
        val entries = root.walkTopDown()
            .filter { shotDir ->
                shotDir.isDirectory && listOf("meta.json", "layout.jpg", "clip.mp4")
                    .all { shotDir.resolve(it).isFile }
            }
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .map { shotDir ->
                val id = shotDir.relativeTo(root).invariantSeparatorsPath
                mapOf(
                    "id" to id,
                    "metaPath" to "/8ball/$id/meta.json",
                    "imagePath" to "/8ball/$id/layout.jpg",
                    "videoPath" to "/8ball/$id/clip.mp4",
                )
            }.toList()

        manifestFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(JsonOutput.prettyPrint(JsonOutput.toJson(entries)) + "\n")
        }
    }
}

tasks.configureEach {
    if (name == "jsProcessResources") {
        dependsOn(generateEightBallManifest)
    }
}

kobweb {
    app {
        index {
            description.set("Predict the next 8-ball shot from real match layouts, then watch the reveal.")
            faviconPath.set("/images/pool-ball-icon.svg")
        }
    }
}

kotlin {
    configAsKobwebApplication("eighballpatterns")

    sourceSets {
        jsMain {
            resources.srcDir(generatedShotResources)
        }

        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(npm("firebase", "13.0.0"))
        }

        jsTest.dependencies {
            implementation(kotlin("test"))
        }

    }
}
