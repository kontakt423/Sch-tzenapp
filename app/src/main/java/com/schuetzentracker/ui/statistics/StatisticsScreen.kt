package com.schuetzentracker.ui.statistics

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.schuetzentracker.data.database.dao.MonthlyStatsRow
import com.schuetzentracker.data.database.dao.WeeklyStatsRow
import com.schuetzentracker.model.DisciplineModel
import com.schuetzentracker.model.TrainingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Statistiken", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Disziplin-Filter ──
            if (uiState.disciplines.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedDiscipline == null,
                                onClick = { viewModel.selectDiscipline(null) },
                                label = { Text("Alle") }
                            )
                        }
                        items(uiState.disciplines) { d ->
                            FilterChip(
                                selected = uiState.selectedDiscipline?.id == d.id,
                                onClick = { viewModel.selectDiscipline(d) },
                                label = { Text(d.shortName.ifBlank { d.name.take(10) }) }
                            )
                        }
                    }
                }
            }

            // ── KPIs ──
            item { KpiRow(uiState) }

            // ── Modus-Verteilung ──
            if (uiState.modeStats.isNotEmpty()) {
                item { ModeDistributionCard(uiState.modeStats) }
            }

            // ── Fortschrittschart ──
            item {
                StatCard("📈 Fortschritt (letzte 12 Wochen)") {
                    if (uiState.weeklyData.isNotEmpty()) {
                        ProgressLineChart(uiState.weeklyData)
                    } else {
                        EmptyChartPlaceholder()
                    }
                }
            }

            // ── Monatschart ──
            item {
                StatCard("📅 Monatsübersicht") {
                    if (uiState.monthlyData.isNotEmpty()) {
                        MonthlyBarChart(uiState.monthlyData)
                    } else {
                        EmptyChartPlaceholder()
                    }
                }
            }

            // ── Korrelationen ──
            item {
                StatCard("😴 Einfluss Ermüdung") {
                    CorrelationTable(uiState.fatigueCorrelation)
                }
            }

            // ── PB & Streak ──
            item { PersonalBestCard(uiState) }
            item { StreakCard(uiState.trainingStreak) }
        }
    }
}

@Composable
fun KpiRow(state: StatisticsUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("Einheiten", state.totalSessions.toString(),        Icons.Default.FitnessCenter),
            Triple("Ø Ringe",   "%.0f".format(state.averageRings),    Icons.Default.TrendingUp),
            Triple("Schüsse",   state.totalShots.toString(),           Icons.Default.GpsFixed)
        ).forEach { (label, value, icon) ->
            Surface(
                modifier      = Modifier.weight(1f),
                shape         = MaterialTheme.shapes.medium,
                color         = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ModeDistributionCard(modeStats: List<Pair<TrainingMode, Int>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Modus-Verteilung", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val total = modeStats.sumOf { it.second }.toFloat()
            modeStats.sortedByDescending { it.second }.forEach { (mode, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${mode.emoji} ${mode.displayName}", Modifier.width(160.dp),
                        style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { if (total > 0) count / total else 0f },
                        modifier = Modifier.weight(1f)
                    )
                    Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun EmptyChartPlaceholder() {
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Text("Noch keine Daten", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProgressLineChart(data: List<WeeklyStatsRow>) {
    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setScaleEnabled(false)
                legend.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { i, row -> Entry(i.toFloat(), row.avgRings.toFloat()) }
            val ds = LineDataSet(entries, "Ø Ringe").apply {
                color = android.graphics.Color.parseColor("#4ADE80")
                lineWidth = 2.5f
                circleRadius = 4f
                setCircleColor(android.graphics.Color.parseColor("#4ADE80"))
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillAlpha = 40
                valueTextColor = android.graphics.Color.parseColor("#4ADE80")
            }
            chart.data = LineData(ds)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}

@Composable
fun MonthlyBarChart(data: List<MonthlyStatsRow>) {
    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                legend.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val reversed = data.reversed()
            val entries = reversed.mapIndexed { i, row -> BarEntry(i.toFloat(), row.avgRings.toFloat()) }
            val labels = reversed.map { it.month.takeLast(5) }
            val ds = BarDataSet(entries, "Ø Ringe").apply {
                color = android.graphics.Color.parseColor("#4ADE80")
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = BarData(ds)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}

@Composable
fun CorrelationTable(data: List<Pair<String, Double>>) {
    if (data.isEmpty()) {
        Text("Noch nicht genug Daten", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        return
    }
    val max = data.maxOf { it.second }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.sortedByDescending { it.second }.forEach { (label, avg) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { (avg / max).toFloat() },
                    modifier = Modifier.weight(1f)
                )
                Text("%.1f".format(avg), style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            }
        }
    }
}

@Composable
fun PersonalBestCard(state: StatisticsUiState) {
    Card(
        shape  = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Persönlicher Beststand",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                state.personalBestDate?.let {
                    Text(
                        "Aufgestellt am $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                    )
                }
            }
            Text(
                state.personalBest?.toString() ?: "–",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StreakCard(streak: Int) {
    val streakColor = when {
        streak >= 30 -> MaterialTheme.colorScheme.error
        streak >= 7  -> MaterialTheme.colorScheme.secondary
        else         -> MaterialTheme.colorScheme.onSurface
    }
    Card(shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Whatshot, null, Modifier.size(16.dp), tint = streakColor)
                    Text(
                        "Trainings-Streak",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    if (streak > 0) "Weiter so!" else "Heute mit Training beginnen!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${streak}d",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = streakColor
            )
        }
    }
}

// ── UI State ──
data class StatisticsUiState(
    val disciplines: List<DisciplineModel> = emptyList(),
    val selectedDiscipline: DisciplineModel? = null,
    val totalSessions: Int = 0,
    val totalShots: Int = 0,
    val averageRings: Double = 0.0,
    val personalBest: Int? = null,
    val personalBestDate: String? = null,
    val weeklyData: List<WeeklyStatsRow> = emptyList(),
    val monthlyData: List<MonthlyStatsRow> = emptyList(),
    val fatigueCorrelation: List<Pair<String, Double>> = emptyList(),
    val modeStats: List<Pair<TrainingMode, Int>> = emptyList(),
    val trainingStreak: Int = 0
)
