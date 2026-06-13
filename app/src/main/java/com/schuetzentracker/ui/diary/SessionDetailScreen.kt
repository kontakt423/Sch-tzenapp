package com.schuetzentracker.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.schuetzentracker.data.repository.TrainingRepository
import com.schuetzentracker.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.session?.disciplineName ?: "Training",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        uiState.session?.let { s ->
                            Text(s.date.format(DateTimeFormatter.ofPattern("dd. MMMM yyyy · HH:mm")),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Zurück") } }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.session == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Training nicht gefunden.")
            }
            else -> {
                val session = uiState.session!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { SessionSummaryCard(session) }
                    if (!session.competitionName.isNullOrBlank()) {
                        item { CompetitionBanner(session.competitionName!!) }
                    }
                    item { ConditionsDetailCard(session) }
                    item {
                        Text("📋 Serien (${session.series.size})",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(session.series) { idx, series ->
                        SeriesDetailCard(idx + 1, series,
                            session.disciplineModel?.maxRingsPerShot ?: 10)
                    }
                    if (session.notes.isNotBlank()) {
                        item { NotesDetailCard(session.notes) }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun SessionSummaryCard(session: TrainingSession) {
    val modeColor = when (session.mode) {
        TrainingMode.COMPETITION   -> MaterialTheme.colorScheme.error
        TrainingMode.QUALIFICATION -> MaterialTheme.colorScheme.tertiary
        TrainingMode.PRACTICE      -> MaterialTheme.colorScheme.secondary
        else                       -> MaterialTheme.colorScheme.primary
    }
    Card(colors = CardDefaults.cardColors(containerColor = modeColor.copy(alpha = 0.12f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = modeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(session.mode.emoji, fontSize = 16.sp)
                        Text(session.mode.displayName, style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = modeColor)
                    }
                }
                Text("${session.series.size} Serien · ${session.totalShots} Schüsse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("📍 ${session.location}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.totalRings}", style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold, color = modeColor)
                Text("Ringe", style = MaterialTheme.typography.labelMedium, color = modeColor)
            }
        }
    }
}

@Composable
fun CompetitionBanner(name: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp))
            Column {
                Text("Wettkampf", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
fun ConditionsDetailCard(session: TrainingSession) {
    val c = session.conditions
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🌤 Bedingungen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CondDetail("Wetter", "${c.weather.emoji} ${c.weather.label}", Modifier.weight(1f))
                CondDetail("Wind", c.wind.label, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CondDetail("Ermüdung", "${c.fatigue.emoji} ${c.fatigue.label}", Modifier.weight(1f))
                CondDetail("Stress", c.stress.label, Modifier.weight(1f))
            }
            if (c.equipment.isNotBlank()) CondDetail("Ausrüstung", c.equipment, Modifier.fillMaxWidth())
            if (c.temperature != null) CondDetail("Temperatur", "${c.temperature}°C", Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CondDetail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SeriesDetailCard(seriesNumber: Int, series: Series, maxRingsPerShot: Int) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Serie $seriesNumber", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("${series.totalRings} / ${series.shots.size * maxRingsPerShot} Ringe",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            series.shots.chunked(5).forEach { rowShots ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowShots.forEach { shot ->
                        val bg = when {
                            shot.rings == maxRingsPerShot     -> MaterialTheme.colorScheme.primary
                            shot.rings >= maxRingsPerShot - 2 -> MaterialTheme.colorScheme.secondary
                            shot.rings >= maxRingsPerShot - 5 -> MaterialTheme.colorScheme.tertiary
                            shot.rings > 0                    -> MaterialTheme.colorScheme.errorContainer
                            else                              -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val fg = when {
                            shot.rings == maxRingsPerShot     -> MaterialTheme.colorScheme.onPrimary
                            shot.rings >= maxRingsPerShot - 2 -> MaterialTheme.colorScheme.onSecondary
                            shot.rings >= maxRingsPerShot - 5 -> MaterialTheme.colorScheme.onTertiary
                            shot.rings > 0                    -> MaterialTheme.colorScheme.onErrorContainer
                            else                              -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(bg),
                            contentAlignment = Alignment.Center) {
                            Text(if (shot.rings == 0) "–" else "${shot.rings}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                    repeat(5 - rowShots.size) { Spacer(Modifier.size(44.dp)) }
                }
            }
            if (series.targetImagePath != null) {
                AsyncImage(model = series.targetImagePath,
                    contentDescription = "Zielscheibe Serie $seriesNumber",
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit)
            }
            series.analysisResult?.let { ai ->
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("🤖 KI-Analyse", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        Text("${ai.shotCount} Schüsse · Trefferbild: ${ai.groupCenter?.name ?: "–"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = if (ai.confidence > 0.8f) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)) {
                        Text("${"%.0f".format(ai.confidence * 100)}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotesDetailCard(notes: String) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📝 Notizen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Text(notes, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
    }
}

// ── ViewModel ────────────────────────────────────────────────────────────

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    fun load(sessionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val session = repository.getSession(sessionId)
            _uiState.update { it.copy(session = session, isLoading = false) }
        }
    }
}

data class SessionDetailUiState(
    val session: TrainingSession? = null,
    val isLoading: Boolean = true
)
