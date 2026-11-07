import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat
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
val rawShotResources = layout.projectDirectory.dir("src/jsMain/resources/public/8ball")

val generateEightBallManifest = tasks.register("generateEightBallManifest") {
    val shotsDir = rawShotResources
    val publicationRoot = generatedShotResources
    inputs.files(fileTree(shotsDir) {
        include("**/meta.json", "**/layout.jpg", "**/clip.mp4", "**/source-info.json")
        exclude("**/*_bad/**", "**/.*/**")
    })
    outputs.dir(publicationRoot)

    doLast {
        val root = shotsDir.asFile
        val outputRoot = publicationRoot.get().asFile.resolve("public/8ball")
        val idPattern = Regex("[A-Za-z0-9_-]{11}/[0-9]{6}(?:_[0-9]+)?")
        // Preflight the entire collection before replacing any generated publication files.
        val plans = root.walkTopDown()
            .onEnter { !it.name.startsWith(".") && !it.name.endsWith("_bad") }
            .filter { it.isDirectory && it.resolve("meta.json").isFile }
            .map { shotDir ->
                require(listOf("layout.jpg", "clip.mp4").all { shotDir.resolve(it).isFile }) {
                    "Annotated shot is missing layout.jpg or clip.mp4: $shotDir"
                }
                val path = shotDir.relativeTo(root).invariantSeparatorsPath
                val parts = path.split('/')
                @Suppress("UNCHECKED_CAST")
                val meta = (JsonSlurper().parse(shotDir.resolve("meta.json")) as Map<String, Any?>).toMutableMap()
                val id = meta["id"] as? String ?: if (parts.size == 4) "${parts[1]}/${parts[3]}" else path
                require(idPattern.matches(id)) { "Invalid shot ID/folder structure: $path" }
                var ancestor: File? = shotDir.parentFile
                while (ancestor != null && ancestor != root.parentFile) {
                    val credits = ancestor.resolve("source-info.json")
                    if (credits.isFile) {
                        val info = JsonSlurper().parse(credits) as Map<*, *>
                        for (field in listOf("source", "videoLink", "ballColors", "browseOrder")) {
                            val missing = meta[field] == null || (meta[field] is String && (meta[field] as String).isBlank())
                            if (missing && info.containsKey(field)) meta[field] = info[field]
                        }
                        break
                    }
                    ancestor = ancestor.parentFile
                }
                val legacy = meta["legacyAssetPath"] as? String
                require(legacy == null || idPattern.matches(legacy)) { "Invalid legacy asset path: $legacy" }
                meta["id"] = id
                Triple(path, id, meta)
            }.toList().sortedBy { it.second }
        require(plans.map { it.second }.distinct().size == plans.size) { "Duplicate shot IDs" }
        // Only this owned, generated subtree may be cleaned; never the user's source folders.
        require(outputRoot.canonicalPath.startsWith(publicationRoot.get().asFile.canonicalPath + File.separator))
        check(!outputRoot.exists() || outputRoot.deleteRecursively()) { "Cannot clean generated shot resources" }
        outputRoot.mkdirs()
        for ((path, _, meta) in plans) {
            val publicationPaths = listOfNotNull(path, (meta["legacyAssetPath"] as? String)?.takeIf { it != path })
            for (publicationPath in publicationPaths) {
                val destination = outputRoot.resolve(publicationPath)
                destination.mkdirs()
                destination.resolve("meta.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(meta)) + "\n")
                for (name in listOf("layout.jpg", "clip.mp4")) root.resolve("$path/$name").copyTo(destination.resolve(name))
            }
        }
        fun versionedPath(path: String, name: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            outputRoot.resolve("$path/$name").inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var count = input.read(buffer)
                while (count != -1) {
                    digest.update(buffer, 0, count)
                    count = input.read(buffer)
                }
            }
            val version = HexFormat.of().formatHex(digest.digest())
            return "/8ball/$path/$name?v=$version"
        }
        val entries = plans.map { (path, id, _) ->
                mapOf(
                    "id" to id,
                    "metaPath" to versionedPath(path, "meta.json"),
                    "imagePath" to versionedPath(path, "layout.jpg"),
                    "videoPath" to versionedPath(path, "clip.mp4"),
                )
            }.toList()

        outputRoot.resolve("index.json").apply {
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
            // Raw match folders may contain source.mp4, screenshots and unreviewed shots.
            // Only the curated generated copies above are published, even after copying a whole match.
            val rawRoot = rawShotResources.asFile.toPath()
            resources.exclude { it.file.toPath().startsWith(rawRoot) }
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
