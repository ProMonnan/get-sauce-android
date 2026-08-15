// Root build script. Plugins are declared with `apply false` here and applied
// per-module below. Versions are chosen for compatibility with AGP 8.6 / Kotlin
// 2.0 / Compose Compiler bundled with Kotlin, which is what stable Android
// Studio Koala Feature Drop and newer expect.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
