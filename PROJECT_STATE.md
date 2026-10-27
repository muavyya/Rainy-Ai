# Rainy AI state
Package com.rainy.ai | Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7 (via Actions, no wrapper), Compose BOM 2024.06.00, minSdk 26
Build: push to main -> GitHub Actions -> artifact RainyAI-debug-apk
Done: Batch 1 (build pipeline, Gemini chat, persona, encrypted API key/model settings). In-memory chat only.
Deps already declared: Room+KSP, OkHttp, serialization, security-crypto, icons-extended.
Next: Batch 2 Room history + settings/privacy; 3 voice; 4 app launch + YouTube; 5 accessibility; 6 screen analysis; 7 coding mode.
Files: ai/{Persona,GeminiClient}.kt data/{ChatMessage,SecureSettings}.kt ui/{Theme,ChatViewModel,ChatScreen,SettingsScreen}.kt MainActivity.kt
