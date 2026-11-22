# Ollama Chat (native Android, Kotlin + Compose)

No login, no accounts, no tokens. Talks directly to an Ollama server over HTTP.

## Build locally
    gradle wrapper --gradle-version 8.9     # once, if gradlew is missing (needs Gradle installed)
    ./gradlew assembleDebug
    # -> app/build/outputs/apk/debug/app-debug.apk

## Build on GitHub
Push to GitHub; the "Build Signed Debug APK" workflow builds, zipaligns and signs the APK
(apksigner, v1 + v2 + v4) and uploads `signed-apk` (APK + .idsig) and `signed-apk-zip`.
Optional repo secrets for your own key: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
Without them a throwaway test key is generated for each build.

## Ollama on your PC
Start with OLLAMA_HOST=0.0.0.0 so the phone can reach it, then set Base URL to http://<PC-IP>:11434.
Note: 127.0.0.1 on a phone is the phone itself; on the Android emulator use http://10.0.2.2:11434.
