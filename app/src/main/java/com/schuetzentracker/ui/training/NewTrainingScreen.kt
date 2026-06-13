package com.schuetzentracker.ui.training

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.schuetzentracker.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTrainingScreen(
    onSaved: () -> Unit,
    onAnalyzeTarget: (Long) -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: NewTrainingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Disziplin", "Serien", "Bedingungen", "Notizen")
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("${uiState.selectedMode.emoji} ${uiState.selectedMode.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Zurück") }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveSession(onSaved) },
                        enabled = uiState.canSave && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Speichern")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                        text = { Text(title) })
                }
            }
            when (selectedTab) {
                0 -> ModeAndDisciplineTab(uiState, viewModel)
                1 -> SeriesTab(uiState, viewModel, onOpenCamera)
                2 -> ConditionsTab(uiState, viewModel)
                3 -> NotesTab(uiState, viewModel)
            }
        }
    }
}

// ────────────────────────────────────────────────
// TAB 1: MODUS & DISZIPLIN
// ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeAndDisciplineTab(state: NewTrainingUiState, vm: NewTrainingViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Modus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingMode.values().forEach { mode ->
                    val isSelected = state.selectedMode == mode
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { vm.selectMode(mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(mode.emoji, style = MaterialTheme.typography.titleLarge)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.displayName, fontWeight = FontWeight.Bold)
                                Text(mode.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected = isSelected, onClick = { vm.selectMode(mode) })
                        }
                    }
                }
            }
        }

        if (state.selectedMode == TrainingMode.COMPETITION ||
            state.selectedMode == TrainingMode.QUALIFICATION) {
            item {
                OutlinedTextField(
                    value = state.competitionName,
                    onValueChange = vm::updateCompetitionName,
                    label = { Text("Wettkampf-Bezeichnung") },
                    placeholder = { Text("z.B. Kreismeisterschaft 2025") },
                    leadingIcon = { Icon(Icons.Default.EmojiEvents, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        }

        item {
            Text("Disziplin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (state.availableDisciplines.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Text("Keine Disziplinen konfiguriert.\nEinstellungen → Disziplinen → Neue anlegen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        } else {
            items(state.availableDisciplines) { discipline ->
                val isSelected = state.selectedDiscipline?.id == discipline.id
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { vm.selectDiscipline(discipline) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder() else null
                ) {
                    Row(modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(discipline.name, fontWeight = FontWeight.Bold)
                            val details = buildList {
                                add("${discipline.shotsPerSeries} Schüsse")
                                add("max. ${discipline.maxRingsTotal} Ringe")
                                add("${discipline.distanceMeters}m")
                                if (discipline.caliber.isNotBlank()) add(discipline.caliber)
                                if (discipline.weaponType.isNotBlank()) add(discipline.weaponType)
                            }.joinToString(" • ")
                            Text(details, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (discipline.hasTimeLimit) {
                                val min = discipline.timeLimitSeconds / 60
                                val sec = discipline.timeLimitSeconds % 60
                                Text("⏱ ${if (min > 0) "${min}min " else ""}${if (sec > 0) "${sec}s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (isSelected) Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Text("Schießstand", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.location,
                onValueChange = vm::updateLocation,
                label = { Text("Ort / Schießstand") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }
    }
}

// ────────────────────────────────────────────────
// TAB 2: SERIEN
// ────────────────────────────────────────────────

@Composable
fun SeriesTab(
    state: NewTrainingUiState,
    vm: NewTrainingViewModel,
    onOpenCamera: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.onTargetImageSelected(it) } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${state.series.size} Serie(n)",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = vm::addSeries, enabled = state.selectedDiscipline != null) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Serie hinzufügen")
                }
            }
        }

        if (state.selectedDiscipline == null) {
            item {
                Text("Bitte zuerst eine Disziplin auswählen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (state.series.isNotEmpty()) {
            item { TotalRingsCard(state) }
        }

        itemsIndexed(state.series) { index, series ->
            SeriesCard(
                seriesNumber = index + 1,
                series       = series,
                discipline   = state.selectedDiscipline,
                onShotChanged = { shotIdx, rings -> vm.updateShot(index, shotIdx, rings) },
                onImagePick  = { vm.setAnalysisSeriesIndex(index); imagePickerLauncher.launch("image/*") },
                onOpenCamera = { vm.setAnalysisSeriesIndex(index); onOpenCamera() },
                onAnalyze    = { vm.analyzeCurrentImage(index) },
                onDelete     = { vm.removeSeries(index) },
                analysisResult = state.analysisResults[index],
                isAnalyzing    = state.isAnalyzing(index)
            )
        }
    }
}

@Composable
fun TotalRingsCard(state: NewTrainingUiState) {
    val disc = state.selectedDiscipline ?: return
    val total = state.series.sumOf { s -> s.shots.sumOf { it } }
    val maxPossible = state.series.size * disc.shotsPerSeries * disc.maxRingsPerShot
    val pct = if (maxPossible > 0) total.toFloat() / maxPossible * 100 else 0f

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Gesamtergebnis", style = MaterialTheme.typography.labelLarge)
                Text("$total von $maxPossible Ringen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            CircularProgressIndicator(
                progress = { pct / 100 },
                modifier = Modifier.size(56.dp),
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesCard(
    seriesNumber: Int,
    series: SeriesUiModel,
    discipline: DisciplineModel?,
    onShotChanged: (Int, Int) -> Unit,
    onImagePick: () -> Unit,
    onOpenCamera: () -> Unit,
    onAnalyze: () -> Unit,
    onDelete: () -> Unit,
    analysisResult: TargetAnalysisResult?,
    isAnalyzing: Boolean = false
) {
    var expanded by remember { mutableStateOf(true) }
    val maxRings = discipline?.maxRingsPerShot ?: 10
    val total = series.shots.sum()
    val maxTotal = (discipline?.shotsPerSeries ?: series.shots.size) * maxRings

    Card {
        Column {
            ListItem(
                headlineContent = { Text("Serie $seriesNumber", fontWeight = FontWeight.Bold) },
                supportingContent = {
                    Text("$total / $maxTotal Ringe", color = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Löschen")
                        }
                    }
                }
            )
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ShotGrid(series.shots, maxRings, onShotChanged)
                    TargetSection(
                        imagePath      = series.targetImagePath,
                        analysisResult = analysisResult,
                        onPickImage    = onImagePick,
                        onOpenCamera   = onOpenCamera,
                        onAnalyze      = onAnalyze,
                        isAnalyzing    = isAnalyzing
                    )
                }
            }
        }
    }
}

@Composable
fun ShotGrid(shots: List<Int>, maxRings: Int, onChanged: (Int, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Schüsse", style = MaterialTheme.typography.labelLarge)
        shots.chunked(5).forEachIndexed { rowIdx, rowShots ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowShots.forEachIndexed { colIdx, rings ->
                    val shotIdx = rowIdx * 5 + colIdx
                    ShotField(shotIdx + 1, rings, maxRings,
                        { newRings -> onChanged(shotIdx, newRings) },
                        Modifier.weight(1f))
                }
                repeat(5 - rowShots.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun ShotField(number: Int, rings: Int, maxRings: Int, onChanged: (Int) -> Unit, modifier: Modifier) {
    val color = when {
        rings == 0        -> MaterialTheme.colorScheme.onSurfaceVariant
        rings == maxRings -> MaterialTheme.colorScheme.primary
        rings >= maxRings - 2 -> MaterialTheme.colorScheme.secondary
        rings >= maxRings - 5 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$number.", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = if (rings == 0) "" else rings.toString(),
            onValueChange = { v -> onChanged(v.toIntOrNull()?.coerceIn(0, maxRings) ?: 0) },
            modifier = Modifier.height(56.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = color, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
fun TargetSection(
    imagePath: String?,
    analysisResult: TargetAnalysisResult?,
    onPickImage: () -> Unit,
    onOpenCamera: () -> Unit,
    onAnalyze: () -> Unit,
    isAnalyzing: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🤖 KI-Scheibenanalyse", style = MaterialTheme.typography.labelLarge)
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Zielscheibe",
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Fit
            )
            when {
                isAnalyzing -> {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Column {
                                Text("KI analysiert Scheibe…", fontWeight = FontWeight.Medium)
                                Text("Erkennt Einschläge und Ringe",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                analysisResult != null -> AnalysisResultMini(analysisResult)
                else -> {
                    Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AutoFixHigh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ringe automatisch erkennen")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenCamera) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Neu fotografieren")
                }
                TextButton(onClick = onPickImage) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Galerie")
                }
            }
        } else {
            Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("📷 Scheibe fotografieren")
            }
            OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("Aus Galerie wählen")
            }
        }
    }
}

@Composable
fun AnalysisResultMini(result: TargetAnalysisResult) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("🤖 Erkannt: ${result.totalRings} Ringe", fontWeight = FontWeight.Bold)
                Text("${result.shotCount} Schüsse${if (result.patchesDetected > 0) " • ${result.patchesDetected} Pflaster" else ""}",
                    style = MaterialTheme.typography.bodySmall)
            }
            Text("${"%.0f".format(result.confidence * 100)}%",
                style = MaterialTheme.typography.labelLarge,
                color = if (result.confidence > 0.8f) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error)
        }
    }
}

// ────────────────────────────────────────────────
// TAB 3: BEDINGUNGEN
// ────────────────────────────────────────────────

@Composable
fun ConditionsTab(state: NewTrainingUiState, vm: NewTrainingViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Wetterbedingungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WeatherCondition.values()) { w ->
                    FilterChip(selected = state.conditions.weather == w,
                        onClick = { vm.updateWeather(w) },
                        label = { Text("${w.emoji} ${w.label}") })
                }
            }
        }
        item {
            Text("Wind", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WindCondition.values()) { w ->
                    FilterChip(selected = state.conditions.wind == w,
                        onClick = { vm.updateWind(w) }, label = { Text(w.label) })
                }
            }
        }
        item {
            Text("Ermüdung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FatigueLevel.values().forEach { f ->
                    FilterChip(selected = state.conditions.fatigue == f,
                        onClick = { vm.updateFatigue(f) },
                        label = { Text("${f.emoji} ${f.label}") },
                        modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Text("Stress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StressLevel.values().forEach { s ->
                    FilterChip(selected = state.conditions.stress == s,
                        onClick = { vm.updateStress(s) },
                        label = { Text(s.label) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.conditions.equipment,
                onValueChange = vm::updateEquipment,
                label = { Text("Waffe / Ausrüstung") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("z.B. LG400 mit Diopter") })
        }
    }
}

// ────────────────────────────────────────────────
// TAB 4: NOTIZEN
// ────────────────────────────────────────────────

@Composable
fun NotesTab(state: NewTrainingUiState, vm: NewTrainingViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Trainings-Notizen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(
                value = state.notes, onValueChange = vm::updateNotes,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("Notizen") },
                placeholder = { Text("Was lief gut? Was verbessern?…") })
        }
        item { Text("Schnellnotizen", style = MaterialTheme.typography.labelLarge) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(
                    "Anschlag gut", "Abzug sauber", "Viel Nachschwingen",
                    "Gute Konzentration", "Windeinfluss", "Visierung geprüft", "Müde"
                )) { note ->
                    SuggestionChip(onClick = { vm.appendNote(note) }, label = { Text(note) })
                }
            }
        }
    }
}
