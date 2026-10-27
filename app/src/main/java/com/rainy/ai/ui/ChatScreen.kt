package com.rainy.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.rainy.ai.data.ChatMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startListening() else vm.onMicDenied()
    }
    val onMicClick: () -> Unit = {
        if (vm.listening) {
            vm.stopListening()
        } else if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            vm.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Move recognized speech into the input box (the user reviews it before sending).
    val transcript = vm.transcript
    LaunchedEffect(transcript) {
        if (transcript != null) {
            input = if (input.isBlank()) transcript else input.trimEnd() + " " + transcript
            vm.consumeTranscript()
        }
    }

    // Never keep the mic open or keep talking when the app goes to the background.
    DisposableEffect(context) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.stopVoice()
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(vm.messages.size, vm.loading) {
        val count = vm.messages.size + if (vm.loading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Chat clear karein?") },
            text = { Text("Is chat ke saare messages delete ho jayenge.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearChat() }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Ye chat delete karein?") },
            text = { Text("Is chat ke saare messages hamesha ke liye delete ho jayenge.") },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; vm.deleteConversation(id) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                Text(
                    "Rainy",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                )
                NavigationDrawerItem(
                    label = { Text("New chat") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    selected = false,
                    onClick = {
                        vm.newChat()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Text(
                    "History",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
                if (vm.conversations.isEmpty()) {
                    Text(
                        "Abhi koi saved chat nahi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(vm.conversations, key = { it.id }) { c ->
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationDrawerItem(
                                label = {
                                    Text(c.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                selected = c.id == vm.activeConversationId,
                                onClick = {
                                    vm.openConversation(c.id)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { deleteTarget = c.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete chat")
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Clear this chat") },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    selected = false,
                    onClick = {
                        confirmClear = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Rainy") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            vm.refreshConversations()
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Chats menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.updateTtsEnabled(!vm.ttsEnabled) }) {
                            Icon(
                                if (vm.ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (vm.ttsEnabled) {
                                    "Spoken replies on. Tap to turn off"
                                } else {
                                    "Spoken replies off. Tap to turn on"
                                }
                            )
                        }
                        IconButton(onClick = { vm.newChat() }, enabled = !vm.loading) {
                            Icon(Icons.Default.Add, contentDescription = "New chat")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(vm.messages, key = { it.id }) { Bubble(it) }
                        if (vm.loading) {
                            item {
                                Text(
                                    "Rainy soch rahi hai...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                    if (vm.historyLoaded && vm.messages.isEmpty() && !vm.loading) {
                        Text(
                            "Hi, main Rainy hoon.\nKuch bhi poochho ya baat shuru karo.",
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val err = vm.error
                if (err != null || (vm.canRetry && !vm.loading)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            err ?: "Pichhle message ka jawab nahi aaya.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (vm.canRetry && !vm.loading) {
                            TextButton(onClick = { vm.retry() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Text("Retry")
                            }
                        }
                    }
                }

                val voiceErr = vm.voiceError
                if (voiceErr != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            voiceErr,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.clearVoiceError() }) { Text("OK") }
                    }
                }

                if (vm.listening) {
                    Text(
                        if (vm.partialText.isBlank()) "Sun rahi hoon..." else vm.partialText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                    )
                }
                if (vm.speaking) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Rainy bol rahi hai...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.stopSpeaking() }) { Text("Stop") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message likho...") },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(onClick = onMicClick) {
                        Icon(
                            if (vm.listening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (vm.listening) "Stop listening" else "Start voice input",
                            tint = if (vm.listening) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    val canSend = input.isNotBlank() && !vm.loading && vm.historyLoaded
                    IconButton(
                        onClick = { vm.send(input); input = "" },
                        enabled = canSend
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val shape = if (msg.isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = shape,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                SelectionContainer { Text(msg.text, style = MaterialTheme.typography.bodyLarge) }
                if (msg.timestamp > 0L) {
                    Text(
                        fmt.format(Date(msg.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
