package com.schuetzentracker.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.schuetzentracker.api.ApiProvider

// ────────────────────────────────────────────────
// OPTIONS / EINSTELLUNGEN SCREEN
// mit dediziertem KI-Analyse-Tab
// ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("🤖 KI-Analyse", "👤 Profil", "🔔 Erinnerungen", "📂 Daten")
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Optionen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AiAnalysisTab(uiState, viewModel)
                1 -> ProfileTab(uiState, viewModel)
                2 -> RemindersTab(uiState, viewModel)
                3 -> DataTab(viewModel)
            }
        }
    }
}

// ────────────────────────────────────────────────
// TAB 1: KI-ANALYSE (Hauptfeature)
// ────────────────────────────────────────────────

@Composable
fun AiAnalysisTab(state: SettingsUiState, vm: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Anbieter-Auswahl ──
        Text(
            "KI-Anbieter auswählen",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        ApiProvider.values().forEach { provider ->
            ProviderCard(
                provider = provider,
                isSelected = state.selectedProvider == provider,
                onClick = { vm.selectProvider(provider) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Gemini Sektion ──
        GeminiKeySection(state, vm)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Claude Sektion ──
        ClaudeKeySection(state, vm)
    }
}

// ── Provider-Auswahl-Karte ────────────────────────

@Composable
fun ProviderCard(provider: ApiProvider, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Provider-Icon
            Text(
                if (provider == ApiProvider.GEMINI) "✨" else "🤖",
                fontSize = 28.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        provider.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (provider == ApiProvider.GEMINI) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "KOSTENLOS",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

// ── Gemini Key Sektion ────────────────────────────

@Composable
fun GeminiKeySection(state: SettingsUiState, vm: SettingsViewModel) {
    var keyInput by remember(state.geminiApiKey) { mutableStateOf(state.geminiApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }

    val hasKey = state.geminiApiKey.isNotBlank()
    val isActive = state.selectedProvider == ApiProvider.GEMINI

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨", fontSize = 20.sp)
                Text(
                    "Google Gemini API-Key",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (hasKey) {
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF2E7D32)
                        )
                        Text(
                            if (isActive) "Aktiv" else "Gespeichert",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // Key-Eingabe
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API-Key eingeben") },
            placeholder = { Text("AIza...") },
            visualTransformation = if (showKey)
                VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Verbergen" else "Anzeigen"
                        )
                    }
                }
            },
            singleLine = true,
            supportingText = {
                Text(
                    "Wird sicher auf deinem Gerät gespeichert.",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )

        // Speichern-Button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.updateGeminiApiKey(keyInput) },
                enabled = keyInput.isNotBlank() && keyInput != state.geminiApiKey,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Speichern")
            }
            if (hasKey) {
                OutlinedButton(
                    onClick = { keyInput = ""; vm.updateGeminiApiKey("") },
                    modifier = Modifier.wrapContentWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                }
            }
        }

        // Anleitung ein/ausklappen
        TextButton(
            onClick = { showInstructions = !showInstructions },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (showInstructions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (showInstructions) "Anleitung verbergen" else "🔑 Wie bekomme ich einen Gemini Key?",
                style = MaterialTheme.typography.labelMedium
            )
        }

        AnimatedVisibility(visible = showInstructions) {
            GeminiInstructions()
        }
    }
}

// ── Gemini Anleitung ──────────────────────────────

@Composable
fun GeminiInstructions() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Gemini API-Key kostenlos holen",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            listOf(
                "1️⃣" to "Browser öffnen und zu folgender Adresse gehen:",
                "🌐" to "aistudio.google.com/app/apikey",
                "2️⃣" to "Mit Google-Account anmelden (kostenlos)",
                "3️⃣" to "\"Create API Key\" klicken",
                "4️⃣" to "Den Key kopieren und oben einfügen",
                "✅" to "Fertig! 1.500 Analysen pro Tag kostenlos"
            ).forEach { (emoji, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(emoji, fontSize = 16.sp, modifier = Modifier.width(28.dp))
                    Text(
                        text,
                        style = if (text.startsWith("aistudio"))
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        else
                            MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    "💡 Gemini 1.5 Flash ist komplett kostenlos:\n" +
                    "• 15 Anfragen pro Minute\n" +
                    "• 1.500 Anfragen pro Tag\n" +
                    "• 1 Million Tokens pro Tag\n" +
                    "Für Scheibenanalysen reicht das für sehr lange!",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ── Claude Key Sektion ────────────────────────────

@Composable
fun ClaudeKeySection(state: SettingsUiState, vm: SettingsViewModel) {
    var keyInput by remember(state.claudeApiKey) { mutableStateOf(state.claudeApiKey) }
    var showKey by remember { mutableStateOf(false) }

    val hasKey = state.claudeApiKey.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤖", fontSize = 20.sp)
                Text(
                    "Anthropic Claude API-Key",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "KOSTENPFLICHTIG",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Text(
            "Optional – nur nötig wenn du Claude statt Gemini nutzen möchtest. " +
            "API-Key unter console.anthropic.com erstellen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Claude API-Key (optional)") },
            placeholder = { Text("sk-ant-api03-...") },
            visualTransformation = if (showKey)
                VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.updateClaudeApiKey(keyInput) },
                enabled = keyInput.isNotBlank() && keyInput != state.claudeApiKey,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Speichern")
            }
            if (hasKey) {
                OutlinedButton(
                    onClick = { keyInput = ""; vm.updateClaudeApiKey("") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// TAB 2: PROFIL
// ────────────────────────────────────────────────

@Composable
fun ProfileTab(state: SettingsUiState, vm: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Schützenprofil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = state.shooterName,
            onValueChange = vm::updateName,
            label = { Text("Dein Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = state.clubName,
            onValueChange = vm::updateClubName,
            label = { Text("Verein") },
            leadingIcon = { Icon(Icons.Default.Groups, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = state.memberNumber,
            onValueChange = vm::updateMemberNumber,
            label = { Text("Mitgliedsnummer") },
            leadingIcon = { Icon(Icons.Default.Badge, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = state.defaultLocation,
            onValueChange = vm::updateDefaultLocation,
            label = { Text("Standard-Schießstand") },
            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
    }
}

// ────────────────────────────────────────────────
// TAB 3: ERINNERUNGEN
// ────────────────────────────────────────────────

@Composable
fun RemindersTab(state: SettingsUiState, vm: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Training-Erinnerungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card {
            ListItem(
                headlineContent = { Text("Tägliche Erinnerung") },
                supportingContent = {
                    Text(
                        if (state.dailyReminderEnabled)
                            "Aktiv – täglich um ${state.reminderHour}:00 Uhr"
                        else "Deaktiviert"
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.dailyReminderEnabled,
                        onCheckedChange = vm::toggleDailyReminder
                    )
                },
                leadingContent = { Icon(Icons.Default.Notifications, null) }
            )
        }

        AnimatedVisibility(visible = state.dailyReminderEnabled) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Uhrzeit: ${state.reminderHour}:00 Uhr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.reminderHour.toFloat(),
                        onValueChange = { vm.setReminderHour(it.toInt()) },
                        valueRange = 6f..22f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("6:00", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("22:00", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// TAB 4: DATEN
// ────────────────────────────────────────────────

@Composable
fun DataTab(vm: SettingsViewModel) {
    val backupMessage by vm.backupMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.importData(it) } }

    LaunchedEffect(backupMessage) {
        backupMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            vm.clearBackupMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Daten & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Trainings exportieren", fontWeight = FontWeight.Medium)
                            Text("Alle Daten als JSON sichern", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { vm.exportData() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Backup erstellen")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text("Backup importieren", fontWeight = FontWeight.Medium)
                            Text("JSON-Backup wiederherstellen", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("JSON-Datei auswählen")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("SchützenTracker v1.0.0\nKI-Analyse mit Google Gemini (kostenlos) oder Anthropic Claude",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
