package com.schuetzentracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schuetzentracker.model.*
import com.schuetzentracker.data.repository.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToDisciplines: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    var nameInput by remember(uiState.shooterName) { mutableStateOf(uiState.shooterName) }
    var clubInput by remember(uiState.clubName) { mutableStateOf(uiState.clubName) }

    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Profil bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Dein Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clubInput,
                        onValueChange = { clubInput = it },
                        label = { Text("Verein") },
                        leadingIcon = { Icon(Icons.Default.Groups, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateShooterName(nameInput)
                    viewModel.updateClubName(clubInput)
                    showEditName = false
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showEditName = false }) { Text("Abbrechen") }
            }
        )
    }

    if (showGoalDialog) {
        AddGoalDialog(
            disciplines = uiState.disciplines,
            onDismiss = { showGoalDialog = false },
            onAdd = { disciplineId, disciplineName, rings, desc ->
                viewModel.addGoal(disciplineId, disciplineName, rings, desc)
                showGoalDialog = false
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("👤 Mein Profil") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                ProfileHeader(uiState, onEditName = { showEditName = true })
            }

            // ── Disziplinen-Button ──
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Disziplinen verwalten") },
                        supportingContent = {
                            Text(
                                if (uiState.disciplines.isEmpty()) "Noch keine Disziplinen – hier anlegen"
                                else "${uiState.disciplines.size} Disziplin(en) konfiguriert"
                            )
                        },
                        leadingContent = { Text("🎯", style = MaterialTheme.typography.titleLarge) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(onClick = onNavigateToDisciplines)
                    )
                }
            }

            // ── Ziele ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯 Trainingsziele", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (uiState.disciplines.isNotEmpty()) {
                        FilledTonalIconButton(onClick = { showGoalDialog = true }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }
            }

            if (uiState.goals.isEmpty()) {
                item {
                    Text("Noch keine Ziele gesetzt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(uiState.goals) { goal ->
                    GoalCard(goal,
                        onDelete = { viewModel.deleteGoal(goal) },
                        onAchieved = { viewModel.markGoalAchieved(goal.id) })
                }
            }

            // ── Achievements ──
            item {
                Text("🏆 Achievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item { AchievementsGrid(uiState.achievements) }

            // ── Disziplin-Statistik ──
            if (uiState.disciplineStats.isNotEmpty()) {
                item {
                    Text("📊 Nach Disziplin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(uiState.disciplineStats) { stat ->
                    DisciplineStatCard(stat)
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(state: ProfileUiState, onEditName: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Text("🎯", style = MaterialTheme.typography.headlineMedium) }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        state.shooterName.ifBlank { "Name eingeben" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, "Bearbeiten",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
                if (state.clubName.isNotBlank()) {
                    Text(state.clubName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
                Text(
                    "${state.totalSessions} Trainings • ${state.unlockedAchievements} Achievements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text("🔥 ${state.streak} Tage Streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun GoalCard(goal: TrainingGoal, onDelete: () -> Unit, onAchieved: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (goal.isAchieved) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            headlineContent = { Text(goal.description, fontWeight = FontWeight.Medium) },
            supportingContent = {
                Column {
                    Text("${goal.disciplineName} • ${goal.targetRings} Ringe",
                        style = MaterialTheme.typography.bodySmall)
                    goal.deadline?.let {
                        Text("Bis: $it", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    goal.achievedDate?.let {
                        Text("✅ Erreicht am $it", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            trailingContent = {
                Row {
                    if (!goal.isAchieved) {
                        IconButton(onClick = onAchieved) {
                            Icon(Icons.Default.CheckCircleOutline, "Ziel erreicht")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Löschen")
                    }
                }
            }
        )
    }
}

@Composable
fun AchievementsGrid(achievements: List<Achievement>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        achievements.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    Card(
                        modifier = Modifier.weight(1f).alpha(if (a.isUnlocked) 1f else 0.3f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (a.isUnlocked) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(a.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(a.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            if (!a.isUnlocked) Icon(Icons.Default.Lock, null, Modifier.size(12.dp))
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun DisciplineStatCard(stat: DisciplineStatInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stat.disciplineName) },
            supportingContent = { Text("${stat.sessions} Einheiten • Ø ${stat.avgRings} Ringe • PB: ${stat.pb ?: "-"}") },
            leadingContent = { Text("🎯") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    disciplines: List<DisciplineModel>,
    onDismiss: () -> Unit,
    onAdd: (Long, String, Int, String) -> Unit
) {
    var selectedDiscipline by remember { mutableStateOf(disciplines.firstOrNull()) }
    var targetRings by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎯 Neues Trainingsziel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedDiscipline?.name ?: "Disziplin wählen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Disziplin") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        disciplines.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.name) },
                                onClick = { selectedDiscipline = d; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetRings,
                    onValueChange = { targetRings = it.filter { c -> c.isDigit() } },
                    label = { Text("Ziel-Ringzahl") },
                    suffix = { Text("Ringe") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung") },
                    placeholder = { Text("z.B. Vereinsmeisterschaft 2025") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val disc = selectedDiscipline ?: return@Button
                    val rings = targetRings.toIntOrNull() ?: return@Button
                    onAdd(disc.id, disc.name, rings, description)
                },
                enabled = selectedDiscipline != null && targetRings.isNotBlank() && description.isNotBlank()
            ) { Text("Ziel setzen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// ── ViewModel ──

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val trainingRepo: TrainingRepository,
    private val goalsRepo: GoalsRepository,
    private val achievementRepo: AchievementRepository,
    private val statsRepo: StatisticsRepository,
    private val disciplineRepo: DisciplineRepository,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        trainingRepo.getAllSessions(),
        goalsRepo.getAllGoals(),
        achievementRepo.getAllAchievements(),
        statsRepo.getTrainingStreak()
    ) { sessions, goals, achievements, streak ->
        Triple(sessions, Pair(goals, achievements), streak)
    }.combine(
        combine(
            statsRepo.getTotalSessionCount(),
            prefsRepo.userPreferences,
            disciplineRepo.getAll()
        ) { count, prefs, disciplines -> Triple(count, prefs, disciplines) }
    ) { (sessions, goalsAndAchievements, streak), (totalCount, prefs, disciplines) ->
        val (goals, achievements) = goalsAndAchievements

        val disciplineStats = disciplines.map { d ->
            val discSessions = sessions.filter { it.disciplineId == d.id }
            val avg = discSessions.map { it.totalRings }.average()
                .let { if (it.isNaN()) "-" else "%.0f".format(it) }
            DisciplineStatInfo(d.name, discSessions.size, avg, discSessions.maxOfOrNull { it.totalRings })
        }.filter { it.sessions > 0 }

        ProfileUiState(
            shooterName = prefs.shooterName,
            clubName = prefs.clubName,
            totalSessions = totalCount,
            streak = streak,
            unlockedAchievements = achievements.count { it.isUnlocked },
            goals = goals,
            achievements = achievements,
            disciplineStats = disciplineStats,
            disciplines = disciplines
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateShooterName(name: String) = viewModelScope.launch { prefsRepo.updateShooterName(name) }
    fun updateClubName(club: String)    = viewModelScope.launch { prefsRepo.updateClubName(club) }

    fun addGoal(disciplineId: Long, disciplineName: String, targetRings: Int, description: String) {
        viewModelScope.launch {
            goalsRepo.saveGoal(TrainingGoal(
                disciplineId = disciplineId,
                disciplineName = disciplineName,
                targetRings = targetRings,
                deadline = null,
                description = description
            ))
        }
    }

    fun deleteGoal(goal: TrainingGoal) = viewModelScope.launch { goalsRepo.deleteGoal(goal) }
    fun markGoalAchieved(goalId: Long) = viewModelScope.launch { goalsRepo.markAchieved(goalId) }
}

// ── Data Classes ──

data class DisciplineStatInfo(
    val disciplineName: String,
    val sessions: Int,
    val avgRings: String,
    val pb: Int?
)

data class ProfileUiState(
    val shooterName: String = "",
    val clubName: String = "",
    val totalSessions: Int = 0,
    val streak: Int = 0,
    val unlockedAchievements: Int = 0,
    val goals: List<TrainingGoal> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val disciplineStats: List<DisciplineStatInfo> = emptyList(),
    val disciplines: List<DisciplineModel> = emptyList()
)
