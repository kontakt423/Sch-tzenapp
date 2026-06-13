package com.schuetzentracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schuetzentracker.model.DisciplineModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineManagementScreen(
    onBack: () -> Unit,
    viewModel: DisciplineManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<DisciplineModel?>(null) }

    if (showDialog || editTarget != null) {
        DisciplineEditDialog(
            existing = editTarget,
            onDismiss = { showDialog = false; editTarget = null },
            onSave = { d ->
                if (d.id == 0L) viewModel.add(d) else viewModel.update(d)
                showDialog = false; editTarget = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 Disziplinen verwalten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editTarget = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Neue Disziplin")
            }
        }
    ) { padding ->
        if (uiState.disciplines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🎯", style = MaterialTheme.typography.displayMedium)
                    Text("Noch keine Disziplinen", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tippe auf + um deine erste Disziplin anzulegen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Erste Disziplin erstellen")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.disciplines, key = { it.id }) { discipline ->
                    DisciplineCard(
                        discipline = discipline,
                        onEdit = { editTarget = discipline },
                        onDelete = { viewModel.delete(discipline) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) } // FAB-Abstand
            }
        }
    }
}

@Composable
fun DisciplineCard(
    discipline: DisciplineModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Disziplin löschen?") },
            text = {
                Text(
                    "\"${discipline.name}\" wird gelöscht. Bestehende Trainings bleiben erhalten."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎯")
                        Text(
                            discipline.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (discipline.shortName.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    discipline.shortName,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DisciplineChip("${discipline.shotsPerSeries} Schüsse")
                        DisciplineChip("max. ${discipline.maxRingsTotal} Ringe")
                        DisciplineChip("${discipline.distanceMeters}m")
                    }
                    if (discipline.caliber.isNotBlank() || discipline.weaponType.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (discipline.weaponType.isNotBlank())
                                DisciplineChip(discipline.weaponType)
                            if (discipline.caliber.isNotBlank())
                                DisciplineChip(discipline.caliber)
                        }
                    }
                    if (discipline.weaponModel.isNotBlank()) {
                        Text(
                            "🔫 ${discipline.weaponModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (discipline.hasTimeLimit) {
                        val min = discipline.timeLimitSeconds / 60
                        val sec = discipline.timeLimitSeconds % 60
                        Text(
                            "⏱ Zeitlimit: ${if (min > 0) "${min}min " else ""}${if (sec > 0) "${sec}s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (discipline.targetDescription.isNotBlank()) {
                        Text(
                            discipline.targetDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete, "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisciplineChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

// ────────────────────────────────────────────────
// BEARBEITUNGS-DIALOG (vollständig)
// ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineEditDialog(
    existing: DisciplineModel?,
    onDismiss: () -> Unit,
    onSave: (DisciplineModel) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var shortName by remember { mutableStateOf(existing?.shortName ?: "") }
    var shotsPerSeries by remember { mutableStateOf(existing?.shotsPerSeries?.toString() ?: "10") }
    var maxRingsPerShot by remember { mutableStateOf(existing?.maxRingsPerShot?.toString() ?: "10") }
    var defaultSeriesCount by remember { mutableStateOf(existing?.defaultSeriesCount?.toString() ?: "1") }
    var caliber by remember { mutableStateOf(existing?.caliber ?: "") }
    var weaponType by remember { mutableStateOf(existing?.weaponType ?: "") }
    var weaponModel by remember { mutableStateOf(existing?.weaponModel ?: "") }
    var distanceMeters by remember { mutableStateOf(existing?.distanceMeters?.toString() ?: "10") }
    var timeLimitMinutes by remember { mutableStateOf(((existing?.timeLimitSeconds ?: 0) / 60).toString()) }
    var timeLimitSeconds by remember { mutableStateOf(((existing?.timeLimitSeconds ?: 0) % 60).toString()) }
    var targetDescription by remember { mutableStateOf(existing?.targetDescription ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var isCompetitionCapable by remember { mutableStateOf(existing?.isCompetitionCapable ?: true) }

    val isValid = name.isNotBlank()
        && shotsPerSeries.toIntOrNull()?.let { it in 1..60 } == true
        && maxRingsPerShot.toIntOrNull()?.let { it in 1..200 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Neue Disziplin" else "Disziplin bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Bezeichnung ──
                SectionLabel("Bezeichnung")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    placeholder = { Text("z.B. Luftgewehr 10m") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank(), singleLine = true
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Kürzel (optional)") },
                    placeholder = { Text("z.B. LG10") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // ── Schussregeln ──
                SectionLabel("Schussregeln")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shotsPerSeries,
                        onValueChange = { shotsPerSeries = it.filter { c -> c.isDigit() } },
                        label = { Text("Schüsse/Serie *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxRingsPerShot,
                        onValueChange = { maxRingsPerShot = it.filter { c -> c.isDigit() } },
                        label = { Text("Max. Ringe *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultSeriesCount,
                        onValueChange = { defaultSeriesCount = it.filter { c -> c.isDigit() } },
                        label = { Text("Standard-Serien") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = distanceMeters,
                        onValueChange = { distanceMeters = it.filter { c -> c.isDigit() } },
                        label = { Text("Distanz (m)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Zeitlimit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = timeLimitMinutes,
                        onValueChange = { timeLimitMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Zeitlimit Min.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timeLimitSeconds,
                        onValueChange = { timeLimitSeconds = it.filter { c -> c.isDigit() } },
                        label = { Text("Sek.") },
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // ── Waffe ──
                SectionLabel("Waffe & Kaliber")
                OutlinedTextField(
                    value = weaponType,
                    onValueChange = { weaponType = it },
                    label = { Text("Waffentyp") },
                    placeholder = { Text("z.B. Luftgewehr, Luftpistole") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = caliber,
                    onValueChange = { caliber = it },
                    label = { Text("Kaliber") },
                    placeholder = { Text("z.B. 4,5mm, 5,6mm (.22lr)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = weaponModel,
                    onValueChange = { weaponModel = it },
                    label = { Text("Waffenmodell (optional)") },
                    placeholder = { Text("z.B. Walther LG400") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                // ── Scheibe & Notizen ──
                SectionLabel("Scheibe & Notizen")
                OutlinedTextField(
                    value = targetDescription,
                    onValueChange = { targetDescription = it },
                    label = { Text("Scheibentyp") },
                    placeholder = { Text("z.B. DSB-Luftgewehrscheibe 10m") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen") },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )

                // ── Wettkampf-Option ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Wettkampf-geeignet", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Kann im Wettkampfmodus verwendet werden",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isCompetitionCapable, onCheckedChange = { isCompetitionCapable = it })
                }

                // Vorschau
                if (isValid) {
                    val shots = shotsPerSeries.toIntOrNull() ?: 10
                    val rings = maxRingsPerShot.toIntOrNull() ?: 10
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "Max. Ergebnis: ${shots * rings} Ringe (${shots} Schüsse × ${rings} Ringe)",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tLimitSec = (timeLimitMinutes.toIntOrNull() ?: 0) * 60 +
                                    (timeLimitSeconds.toIntOrNull() ?: 0)
                    onSave(
                        DisciplineModel(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            shortName = shortName.trim(),
                            shotsPerSeries = shotsPerSeries.toIntOrNull() ?: 10,
                            maxRingsPerShot = maxRingsPerShot.toIntOrNull() ?: 10,
                            defaultSeriesCount = defaultSeriesCount.toIntOrNull() ?: 1,
                            caliber = caliber.trim(),
                            weaponType = weaponType.trim(),
                            weaponModel = weaponModel.trim(),
                            distanceMeters = distanceMeters.toIntOrNull() ?: 10,
                            timeLimitSeconds = tLimitSec,
                            targetDescription = targetDescription.trim(),
                            notes = notes.trim(),
                            isCompetitionCapable = isCompetitionCapable
                        )
                    )
                },
                enabled = isValid
            ) { Text(if (existing == null) "Erstellen" else "Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

data class DisciplineManagementUiState(
    val disciplines: List<DisciplineModel> = emptyList()
)
