package com.schuetzentracker.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schuetzentracker.model.*
import java.time.format.DateTimeFormatter

// ── Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onSessionClick: (Long) -> Unit = {},
    viewModel: DiaryViewModelImpl = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<TrainingSession?>(null) }

    showDeleteDialog?.let { session ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Training löschen?") },
            text = {
                Text("${session.disciplineName} vom ${session.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} wird gelöscht.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteSession(session.id); showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("📓 Trainings-Tagebuch") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Suche ──
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; viewModel.search(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Suchen…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // ── Modus-Filter ──
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedMode == null,
                            onClick = { viewModel.filterByMode(null) },
                            label = { Text("Alle") }
                        )
                    }
                    items(TrainingMode.values()) { mode ->
                        FilterChip(
                            selected = uiState.selectedMode == mode,
                            onClick = { viewModel.filterByMode(mode) },
                            label = { Text("${mode.emoji} ${mode.displayName}") }
                        )
                    }
                }
            }

            // ── Statistik-Chips ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {},
                        label = { Text("${uiState.sessions.size} Einheiten") },
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, null, Modifier.size(16.dp)) })
                    if (uiState.averageRings > 0) {
                        AssistChip(onClick = {},
                            label = { Text("Ø ${"%.0f".format(uiState.averageRings)} Ringe") },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, null, Modifier.size(16.dp)) })
                    }
                }
            }

            // ── Einträge ──
            if (uiState.sessions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Keine Einträge gefunden", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                uiState.groupedSessions.forEach { (month, sessions) ->
                    item {
                        Text(month, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(sessions, key = { it.id }) { session ->
                        DiaryEntryCard(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            onDelete = { showDeleteDialog = session }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryEntryCard(session: TrainingSession, onClick: () -> Unit, onDelete: () -> Unit) {
    val modeColor = when (session.mode) {
        TrainingMode.COMPETITION   -> MaterialTheme.colorScheme.error
        TrainingMode.QUALIFICATION -> MaterialTheme.colorScheme.tertiary
        TrainingMode.PRACTICE      -> MaterialTheme.colorScheme.secondary
        else                       -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Datum
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Text(session.date.format(DateTimeFormatter.ofPattern("dd")),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(session.date.format(DateTimeFormatter.ofPattern("MMM")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(modifier = Modifier.height(56.dp).padding(horizontal = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(session.mode.emoji, style = MaterialTheme.typography.labelMedium)
                    Text(session.disciplineName.ifBlank { "Unbekannt" },
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                Text(
                    "${session.date.format(DateTimeFormatter.ofPattern("HH:mm"))} • ${session.location} • ${session.series.size} Serien",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.competitionName?.isNotBlank() == true) {
                    Text(session.competitionName, style = MaterialTheme.typography.bodySmall, color = modeColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.conditions.weather.emoji, style = MaterialTheme.typography.labelSmall)
                    Text(session.conditions.fatigue.emoji, style = MaterialTheme.typography.labelSmall)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${session.totalRings}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = modeColor)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Löschen",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class DiaryUiState(
    val sessions: List<TrainingSession> = emptyList(),
    val groupedSessions: Map<String, List<TrainingSession>> = emptyMap(),
    val selectedMode: TrainingMode? = null,
    val averageRings: Double = 0.0,
    val isLoading: Boolean = true
)
