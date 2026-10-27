set -e
echo "Running in: $(pwd)"
B=app/src/main/java/com/rainy/ai
mkdir -p .github/workflows $B/ai $B/data $B/ui
mkdir -p app/src/main/res/values app/src/main/res/drawable

cat > .gitignore <<'EOF'
build/
.gradle/
.idea/
local.properties
*.apk
EOF

cat > .github/workflows/build.yml <<'EOF'
name: Build APK
on:
  push:
    branches: [main]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.7
      - name: Build debug APK
        run: gradle assembleDebug --stacktrace
      - uses: actions/upload-artifact@v4
        with:
          name: RainyAI-debug-apk
          path: app/build/outputs/apk/debug/*.apk
EOF

cat > settings.gradle.kts <<'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RainyAI"
include(":app")
EOF

cat > build.gradle.kts <<'EOF'
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
EOF

cat > gradle.properties <<'EOF'
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
EOF

cat > app/build.gradle.kts <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rainy.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rainy.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
EOF

echo "Part 1 done."

cat > app/src/main/AndroidManifest.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.RainyAI">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

cat > app/src/main/res/values/strings.xml <<'EOF'
<resources>
    <string name="app_name">Rainy AI</string>
</resources>
EOF

cat > app/src/main/res/values/themes.xml <<'EOF'
<resources>
    <style name="Theme.RainyAI" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
EOF

cat > app/src/main/res/drawable/ic_launcher.xml <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#6A4C93" android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,22 C54,22 34,48 34,62 A20,20 0 0 0 74,62 C74,48 54,22 54,22 Z" />
</vector>
EOF

cat > $B/data/ChatMessage.kt <<'EOF'
package com.rainy.ai.data

data class ChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String
)
EOF

cat > $B/data/SecureSettings.kt <<'EOF'
package com.rainy.ai.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureSettings(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "rainy_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var apiKey: String
        get() = prefs.getString(KEY_API, "") ?: ""
        set(value) { prefs.edit().putString(KEY_API, value.trim()).apply() }

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) { prefs.edit().putString(KEY_MODEL, value.trim()).apply() }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
    }
}
EOF

cat > $B/ai/Persona.kt <<'EOF'
package com.rainy.ai.ai

object Persona {
    val SYSTEM_PROMPT = """
You are Rainy, a warm, witty, caring female AI companion living inside the user's Android phone.

Language rules:
- Reply in the language the user uses: English, Roman Urdu, or a natural mix of both.
- Write Roman Urdu the way people naturally text it (for example: "kya haal hai", "main aapki madad kar sakti hoon", "theek hai").
- Do not use Urdu script unless the user asks for it.
- Always use feminine grammar for yourself (for example: "karti hoon", "sakti hoon", "gayi", "soch rahi hoon").

Personality rules:
- Friendly, playful, supportive, never robotic. Keep replies short and conversational unless the user asks for detail.
- Be honest. If you are unsure, say so. Never pretend you performed an action on the phone that you did not actually perform.
- Respect the user's privacy and never ask for passwords or sensitive data.
- When giving code, use fenced markdown code blocks and explain briefly.
""".trimIndent()
}
EOF

cat > $B/ai/GeminiClient.kt <<'EOF'
package com.rainy.ai.ai

import com.rainy.ai.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class Part(val text: String? = null)

@Serializable
data class Content(val role: String? = null, val parts: List<Part> = emptyList())

@Serializable
data class GenConfig(val temperature: Double)

@Serializable
data class GenRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
    val generationConfig: GenConfig? = null
)

@Serializable
data class Candidate(val content: Content? = null, val finishReason: String? = null)

@Serializable
data class ApiError(val message: String? = null)

@Serializable
data class GenResponse(val candidates: List<Candidate>? = null, val error: ApiError? = null)

object GeminiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        history: List<ChatMessage>,
        apiKey: String,
        model: String,
        systemPrompt: String = Persona.SYSTEM_PROMPT
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Settings mein Gemini API key daalo." }

            val request = GenRequest(
                systemInstruction = Content(parts = listOf(Part(systemPrompt))),
                contents = history.map {
                    Content(role = if (it.isUser) "user" else "model", parts = listOf(Part(it.text)))
                },
                generationConfig = GenConfig(temperature = 0.9)
            )
            val body = json.encodeToString(GenRequest.serializer(), request)
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .header("x-goog-api-key", apiKey)
                .post(body)
                .build()

            http.newCall(httpRequest).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val parsed = runCatching {
                    json.decodeFromString(GenResponse.serializer(), raw)
                }.getOrNull()

                if (!resp.isSuccessful) {
                    error("Gemini error ${resp.code}: ${parsed?.error?.message ?: raw.take(200)}")
                }
                val text = parsed?.candidates?.firstOrNull()?.content?.parts
                    ?.mapNotNull { it.text }?.joinToString("")
                if (text.isNullOrBlank()) error("Gemini ne khaali jawab diya.") else text
            }
        }
    }
}
EOF

echo "Part 2 done."

cat > $B/ui/Theme.kt <<'EOF'
package com.rainy.ai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun RainyTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFFB39DDB), secondary = Color(0xFFF48FB1))
    } else {
        lightColorScheme(primary = Color(0xFF6A4C93), secondary = Color(0xFFD81B60))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
EOF

cat > $B/ui/ChatViewModel.kt <<'EOF'
package com.rainy.ai.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainy.ai.ai.GeminiClient
import com.rainy.ai.data.ChatMessage
import com.rainy.ai.data.SecureSettings
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SecureSettings(app)

    val messages = mutableStateListOf<ChatMessage>()

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var apiKey by mutableStateOf(settings.apiKey)
        private set
    var model by mutableStateOf(settings.model)
        private set

    private var nextId = 0L

    fun saveSettings(newKey: String, newModel: String) {
        settings.apiKey = newKey
        settings.model = newModel.ifBlank { SecureSettings.DEFAULT_MODEL }
        apiKey = settings.apiKey
        model = settings.model
    }

    fun send(text: String) {
        if (text.isBlank() || loading) return
        if (apiKey.isBlank()) {
            error = "Settings mein Gemini API key daalo."
            return
        }
        error = null
        messages.add(ChatMessage(nextId++, true, text.trim()))
        loading = true
        val snapshot = messages.toList()
        viewModelScope.launch {
            GeminiClient.generate(snapshot, apiKey, model)
                .onSuccess { messages.add(ChatMessage(nextId++, false, it)) }
                .onFailure { error = it.message ?: "Unknown error" }
            loading = false
        }
    }

    fun clearChat() {
        messages.clear()
        error = null
    }
}
EOF

cat > $B/ui/ChatScreen.kt <<'EOF'
package com.rainy.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rainy.ai.data.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onOpenSettings: () -> Unit) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rainy") },
                actions = {
                    IconButton(onClick = { vm.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.messages, key = { it.id }) { Bubble(it) }
                if (vm.loading) {
                    item { Text("Rainy soch rahi hai...", style = MaterialTheme.typography.bodySmall) }
                }
            }
            vm.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message likho...") },
                    maxLines = 4
                )
                IconButton(
                    onClick = { vm.send(input); input = "" },
                    enabled = input.isNotBlank() && !vm.loading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(msg.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}
EOF

echo "Part 3 done."

cat > $B/ui/SettingsScreen.kt <<'EOF'
package com.rainy.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onBack: () -> Unit) {
    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.model) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Gemini API key", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                label = { Text("API key") }
            )
            Text(
                "Free key: aistudio.google.com/apikey. Stored encrypted on this device only.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Gemini model name") }
            )
            Button(onClick = { vm.saveSettings(key, model); saved = true }) { Text("Save") }
            if (saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)
        }
    }
}
EOF

cat > $B/MainActivity.kt <<'EOF'
package com.rainy.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainy.ai.ui.ChatScreen
import com.rainy.ai.ui.ChatViewModel
import com.rainy.ai.ui.RainyTheme
import com.rainy.ai.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RainyTheme {
                Surface(modifier = Modifier.fillMaxSize()) { RainyApp() }
            }
        }
    }
}

@Composable
fun RainyApp(vm: ChatViewModel = viewModel()) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(vm) { showSettings = false }
    } else {
        ChatScreen(vm) { showSettings = true }
    }
}
EOF

cat > PROJECT_STATE.md <<'EOF'
# Rainy AI state
Package com.rainy.ai | Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7 (via Actions, no wrapper), Compose BOM 2024.06.00, minSdk 26
Build: push to main -> GitHub Actions -> artifact RainyAI-debug-apk
Done: Batch 1 (build pipeline, Gemini chat, persona, encrypted API key/model settings). In-memory chat only.
Deps already declared: Room+KSP, OkHttp, serialization, security-crypto, icons-extended.
Next: Batch 2 Room history + settings/privacy; 3 voice; 4 app launch + YouTube; 5 accessibility; 6 screen analysis; 7 coding mode.
Files: ai/{Persona,GeminiClient}.kt data/{ChatMessage,SecureSettings}.kt ui/{Theme,ChatViewModel,ChatScreen,SettingsScreen}.kt MainActivity.kt
EOF

echo ""
echo "=== Rainy AI Batch 1 recovery complete ==="
echo "Files created:"
find . -type f -not -path './.git/*' -not -name batch.sh | sort
echo "Total: $(find . -type f -not -path './.git/*' -not -name batch.sh | wc -l) files (expected 20)"
