# Ollama Chat (native Android, Kotlin + Compose)

No login, no accounts, no tokens. Talks directly to an Ollama server over HTTP.

## Build locally
    gradle wrapper --gradle-version 8.9     # once, if gradlew is missing (needs Gradle installed)
    ./gradlew assembleDebug
    # -> app/build/outputs/apk/debug/app-debug.apk

## Build on GitHub
Push to GitHub; the "Build debug APK" workflow uploads app-debug.apk as an artifact.

## Ollama on your PC
Start with OLLAMA_HOST=0.0.0.0 so the phone can reach it, then set Base URL to http://<PC-IP>:11434.
Note: 127.0.0.1 on a phone is the phone itself; on the Android emulator use http://10.0.2.2:11434.
