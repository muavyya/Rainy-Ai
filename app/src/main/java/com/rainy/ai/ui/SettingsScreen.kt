package com.rainy.ai.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val MODEL_REGEX = Regex("^[A-Za-z0-9._-]+$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val context = LocalContext.current

    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.model) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }

    val cleanKey = key.trim()
    val cleanModel = model.trim().removePrefix("models/")
    val keyProblem: String? = when {
        cleanKey.isEmpty() -> "API key zaroori hai."
        cleanKey.any { it.isWhitespace() } -> "Key mein space nahi hona chahiye."
        cleanKey.length < 20 -> "Key bohat chhoti lag rahi hai."
        else -> null
    }
    val modelProblem: String? = when {
        cleanModel.isEmpty() -> "Model name zaroori hai."
        !MODEL_REGEX.matches(cleanModel) -> "Model name mein sirf letters, numbers, . _ - allowed hain."
        else -> null
    }
    val valid = keyProblem == null && modelProblem == null

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Saari history delete karein?") },
            text = { Text("Saari chats aur messages hamesha ke liye delete ho jayenge. Ye undo nahi hoga.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAllHistory()
                    info = "Saari history delete kar di gayi."
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

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
            Text("Gemini", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = key,
                onValueChange = { key = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("API key") },
                isError = key.isNotEmpty() && keyProblem != null,
                supportingText = {
                    if (key.isNotEmpty() && keyProblem != null) Text(keyProblem)
                },
                visualTransformation =
                    if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Show or hide key"
                        )
                    }
                }
            )
            Text(
                "Free key: aistudio.google.com/apikey. Key is device par encrypted save hoti hai " +
                    "aur sirf Google Gemini ko bheji jati hai.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = model,
                onValueChange = { model = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Gemini model name") },
                isError = modelProblem != null,
                supportingText = { if (modelProblem != null) Text(modelProblem) }
            )

            Box {
                OutlinedButton(
                    onClick = { menuOpen = true },
                    enabled = vm.models.isNotEmpty()
                ) {
                    Text(
                        if (vm.models.isEmpty()) "Pehle models load karein"
                        else "Models mein se chunein (${vm.models.size})"
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    vm.models.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                model = name
                                saved = false
                                menuOpen = false
                            }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { vm.loadModels(cleanKey) },
                enabled = !vm.testing && keyProblem == null
            ) { Text("Models load karein") }

            OutlinedButton(
                onClick = { vm.testConnection(cleanKey, cleanModel) },
                enabled = !vm.testing && valid
            ) { Text(if (vm.testing) "Please wait..." else "Test connection") }

            vm.testStatus?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (vm.testOk) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Button(
                onClick = {
                    vm.saveSettings(cleanKey, cleanModel)
                    saved = true
                },
                enabled = valid
            ) { Text("Save") }
            if (saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)

            HorizontalDivider()

            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "Chats sirf is phone par save hoti hain. Jab aap message bhejte hain, chat " +
                    "history Google Gemini ko jawab ke liye bheji jati hai.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(onClick = {
                vm.exportText { text ->
                    if (text.isBlank()) {
                        info = "Export ke liye koi chat nahi hai."
                    } else {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Chat export"))
                    }
                }
            }) { Text("Current chat export karein") }

            OutlinedButton(
                onClick = { confirmDelete = true }
            ) { Text("Saari history delete karein") }

            info?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
