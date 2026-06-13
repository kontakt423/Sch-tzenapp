package com.schuetzentracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schuetzentracker.model.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartTraining: () -> Unit,
    onViewSession: (Long) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 SchützenTracker") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Einstellungen")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Kein Disziplin → Onboarding ──
            if (uiState.noDisciplinesConfigured) {
                item { OnboardingCard(onOpenSettings) }
            } else {
                item { QuickStartCard(onStartTraining) }
            }

            // ── Statistik ──
            item { StatsOverviewCard(uiState) }

            // ── Letzte Trainings ──
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    Text(
                        "📅 Zuletzt trainiert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.recentSessions) { session ->
                    RecentSessionCard(session, onClick = { onViewSession(session.id) })
                }
            } else if (!uiState.noDisciplinesConfigured) {
                item { EmptySessionsCard(onStartTraining) }
            }

            // ── Achievements ──
            if (uiState.newAchievements.isNotEmpty()) {
                item {
                    Text(
                        "🏆 Neu freigeschaltet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.newAchievements) { achievement ->
                    AchievementCard(achievement)
                }
            }
        }
    }
}

@Composable
fun OnboardingCard(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", style = MaterialTheme.typography.displayMedium)
            Text(
                "Willkommen beim SchützenTracker!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Lege zuerst deine Disziplinen an, bevor du mit dem Training beginnst.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Button(onClick = onOpenSettings) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Erste Disziplin anlegen")
            }
        }
    }
}

@Composable
fun QuickStartCard(onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Bereit zum Trainieren?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Neue Einheit starten",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            FilledIconButton(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, "Training starten")
            }
        }
    }
}

@Composable
fun StatsOverviewCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📊 Deine Statistik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Trainings",    state.totalSessions.toString(), "gesamt")
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem("Ø Ringe",     "%.1f".format(state.averageRings), "letzte 10")
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem("🔥 Streak",   "${state.trainingStreak}d", "aktiv")
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem("Wettkämpfe",  state.competitionCount.toString(), "gesamt")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(sub,   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecentSessionCard(session: TrainingSession, onClick: () -> Unit) {
    val dtFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val modeColor = when (session.mode) {
        TrainingMode.COMPETITION  -> MaterialTheme.colorScheme.error
        TrainingMode.QUALIFICATION -> MaterialTheme.colorScheme.tertiary
        TrainingMode.PRACTICE     -> MaterialTheme.colorScheme.secondary
        else                      -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(session.mode.emoji, style = MaterialTheme.typography.titleSmall)
                    Text(
                        session.disciplineName.ifBlank { "Unbekannte Disziplin" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    session.date.format(dtFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.competitionName?.isNotBlank() == true) {
                    Text(
                        session.competitionName,
                        style = MaterialTheme.typography.bodySmall,
                        color = modeColor
                    )
                }
                Text(
                    "${session.series.size} Serien • ${session.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${session.totalRings}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = modeColor
                )
                Text(
                    session.mode.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = modeColor
                )
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(achievement.emoji, style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(achievement.title, fontWeight = FontWeight.Bold)
                Text(achievement.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun EmptySessionsCard(onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", style = MaterialTheme.typography.displayMedium)
            Text("Noch kein Training", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Starte deine erste Trainingseinheit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onStart) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Erstes Training starten")
            }
        }
    }
}

// ── UI State ──
data class HomeUiState(
    val totalSessions: Int = 0,
    val averageRings: Double = 0.0,
    val trainingStreak: Int = 0,
    val competitionCount: Int = 0,
    val recentSessions: List<TrainingSession> = emptyList(),
    val newAchievements: List<Achievement> = emptyList(),
    val noDisciplinesConfigured: Boolean = false,
    val isLoading: Boolean = true
)
