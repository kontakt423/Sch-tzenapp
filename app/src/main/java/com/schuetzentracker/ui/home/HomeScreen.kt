package com.schuetzentracker.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SchützenTracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hero / Onboarding ──────────────────────────────────
            if (uiState.noDisciplinesConfigured) {
                item { OnboardingHero(onOpenSettings) }
            } else {
                item { HeroCard(onStartTraining) }
            }

            // ── Statistik-Chips ────────────────────────────────────
            if (!uiState.isLoading) {
                item { StatsRow(uiState) }
            }

            // ── Letzte Trainings ───────────────────────────────────
            if (uiState.recentSessions.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon   = Icons.Default.History,
                        title  = "Letzte Trainings"
                    )
                }
                items(uiState.recentSessions) { session ->
                    SessionCard(session, onClick = { onViewSession(session.id) })
                }
            } else if (!uiState.noDisciplinesConfigured && !uiState.isLoading) {
                item { EmptySessionsHint(onStartTraining) }
            }

            // ── Achievements ───────────────────────────────────────
            if (uiState.newAchievements.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon  = Icons.Default.EmojiEvents,
                        title = "Freigeschaltet"
                    )
                }
                items(uiState.newAchievements) { achievement ->
                    AchievementItem(achievement)
                }
            }

            item { Spacer(Modifier.height(88.dp)) } // FAB-Puffer
        }
    }
}

// ── HERO CARD ─────────────────────────────────────────────────────────

@Composable
fun HeroCard(onStart: () -> Unit) {
    val colorStart = MaterialTheme.colorScheme.primary
    val colorEnd   = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(colorStart, colorEnd)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Bereit fürs\nnächste Training?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Dokumentiere jeden Schuss.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }
            Spacer(Modifier.width(16.dp))
            FilledIconButton(
                onClick = onStart,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor   = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Training starten",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ── ONBOARDING ────────────────────────────────────────────────────────

@Composable
fun OnboardingHero(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.GpsFixed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
            Text(
                "Willkommen!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Lege deine erste Disziplin an, um mit dem Training zu beginnen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onOpenSettings,
                shape   = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Erste Disziplin anlegen", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── STATISTIK CHIPS ───────────────────────────────────────────────────

@Composable
fun StatsRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(value = "${state.totalSessions}",              label = "Einheiten",  icon = Icons.Default.FitnessCenter, modifier = Modifier.weight(1f))
        StatChip(value = "%.0f".format(state.averageRings),     label = "⌀ Ringe",    icon = Icons.Default.GpsFixed,      modifier = Modifier.weight(1f))
        StatChip(value = "${state.trainingStreak}",             label = "Tage-Serie", icon = Icons.Default.Whatshot,     modifier = Modifier.weight(1f))
        StatChip(value = "${state.competitionCount}",           label = "Wettkämpfe", icon = Icons.Default.EmojiEvents,   modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatChip(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier      = modifier,
        shape         = MaterialTheme.shapes.medium,
        color         = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ── SECTION HEADER ────────────────────────────────────────────────────

@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── SESSION CARD ──────────────────────────────────────────────────────

@Composable
fun SessionCard(session: TrainingSession, onClick: () -> Unit) {
    val dayFmt  = DateTimeFormatter.ofPattern("dd")
    val monFmt  = DateTimeFormatter.ofPattern("MMM")
    val modeColor = when (session.mode) {
        TrainingMode.COMPETITION   -> MaterialTheme.colorScheme.error
        TrainingMode.QUALIFICATION -> MaterialTheme.colorScheme.tertiary
        TrainingMode.PRACTICE      -> MaterialTheme.colorScheme.secondary
        else                       -> MaterialTheme.colorScheme.primary
    }
    val ringFraction = if (session.maxPossible > 0)
        session.totalRings.toFloat() / session.maxPossible else 0f
    val ringColor = when {
        ringFraction >= 0.95f -> MaterialTheme.colorScheme.primary
        ringFraction >= 0.85f -> MaterialTheme.colorScheme.secondary
        else                  -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Datum-Badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        session.date.format(dayFmt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        session.date.format(monFmt).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Inhalt
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    session.disciplineName.ifBlank { "Unbekannte Disziplin" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${session.mode.emoji} ${session.mode.displayName}  ·  ${session.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.competitionName?.isNotBlank() == true) {
                    Text(
                        session.competitionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = modeColor
                    )
                }
            }

            // Ringwert
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${session.totalRings}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ringColor
                )
                Text(
                    "Ringe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── EMPTY STATE ───────────────────────────────────────────────────────

@Composable
fun EmptySessionsHint(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.GpsFixed, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            "Noch kein Training",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Starte deine erste Einheit und\nbeobachte deinen Fortschritt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onStart, shape = MaterialTheme.shapes.medium) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Erstes Training starten", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── ACHIEVEMENT ───────────────────────────────────────────────────────

@Composable
fun AchievementItem(achievement: Achievement) {
    Surface(
        modifier      = Modifier.fillMaxWidth(),
        shape         = MaterialTheme.shapes.medium,
        color         = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(achievement.emoji, fontSize = 22.sp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.Star, null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── UI STATE ──────────────────────────────────────────────────────────

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
