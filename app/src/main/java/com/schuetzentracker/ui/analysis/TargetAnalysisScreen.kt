package com.schuetzentracker.ui.analysis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.graphics.Paint as AndroidPaint
import com.schuetzentracker.model.DetectedShot
import com.schuetzentracker.model.ShotZone
import com.schuetzentracker.model.TargetAnalysisResult
import androidx.compose.ui.graphics.nativeCanvas


// ────────────────────────────────────────────────
// TARGET ANALYSIS SCREEN – vollständig
// ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetAnalysisScreen(
    seriesId: Long,
    onBack: () -> Unit,
    viewModel: TargetAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.load(seriesId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 KI-Scheibenanalyse") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    if (uiState.result != null) {
                        IconButton(onClick = viewModel::applyResultToShots) {
                            Icon(Icons.Default.CheckCircle, "Ergebnis übernehmen")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                uiState.isAnalyzing -> AnalyzingLoadingState()
                uiState.error != null -> ErrorState(uiState.error!!, onRetry = viewModel::retry)
                uiState.result != null -> AnalysisResultContent(
                    result = uiState.result!!,
                    imagePath = uiState.imagePath,
                    rings = uiState.maxRingsPerShot
                )
                else -> EmptyAnalysisState()
            }
        }
    }
}

// ────────────────────────────────────────────────
// LADE-ANIMATION
// ────────────────────────────────────────────────

@Composable
fun AnalyzingLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(80.dp), strokeWidth = 3.dp)
            Text("🤖", fontSize = 32.sp)
        }
        Text(
            "KI analysiert deine Scheibe…",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "Claude Vision erkennt Einschläge und Schusspflaster.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        listOf(
            "📷 Bild wird analysiert",
            "🔍 Einschläge werden gesucht",
            "⬜ Schusspflaster werden erkannt",
            "🔢 Ringe werden gezählt"
        ).forEachIndexed { i, step ->
            val stepAlpha by rememberInfiniteTransition(label = "step$i").animateFloat(
                initialValue = 0.2f, targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    tween(800, delayMillis = i * 200),
                    RepeatMode.Reverse
                ),
                label = "stepAlpha$i"
            )
            Text(
                step,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = stepAlpha)
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("❌", fontSize = 48.sp)
        Text("Analyse fehlgeschlagen", style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
                message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Erneut versuchen")
        }
        Text(
            "Tipp: Bessere Beleuchtung und scharfer Fokus verbessern die Erkennung deutlich.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyAnalysisState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📷", fontSize = 48.sp)
        Text("Kein Scheibenfoto", style = MaterialTheme.typography.titleMedium)
        Text(
            "Lade ein Foto deiner Zielscheibe hoch, um die KI-Analyse zu starten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ────────────────────────────────────────────────
// ANALYSE-ERGEBNIS – Haupt-Content
// ────────────────────────────────────────────────

@Composable
fun AnalysisResultContent(
    result: TargetAnalysisResult,
    imagePath: String?,
    rings: Int
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScoreBanner(result)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (imagePath != null) {
                Card(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            "📷 Original",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                        ZoomableImage(
                            imagePath = imagePath,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        "🤖 KI-Overlay",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                    TargetVisualizationCanvas(
                        shots = result.detectedRings,
                        maxRings = rings,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }
        }

        AnalysisDetailCard(result)

        if (result.detectedRings.isNotEmpty()) {
            ShotBreakdownCard(result.detectedRings)
        }

        result.groupCenter?.let { zone -> TipsCard(zone) }
    }
}

// ────────────────────────────────────────────────
// SCORE BANNER
// ────────────────────────────────────────────────

@Composable
fun ScoreBanner(result: TargetAnalysisResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Erkannte Ringe",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    "${result.totalRings}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (result.patchesDetected > 0) {
                    Text(
                        "inkl. ${result.patchRings} Ringe aus ${result.patchesDetected} Pflaster(n)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfidenceBadge(result.confidence)
                Text(
                    "${result.shotCount} Schüsse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Float) {
    val pct = (confidence * 100).toInt()
    val color = when {
        pct >= 85 -> Color(0xFF2E7D32)
        pct >= 60 -> Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.error
    }
    val label = when {
        pct >= 85 -> "Hohe Konfidenz"
        pct >= 60 -> "Mittlere Konfidenz"
        else -> "Bitte prüfen"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.small))
            Text("$pct% – $label", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

// ────────────────────────────────────────────────
// CANVAS: ZIELSCHEIBE MIT EINSCHLÄGEN
// ────────────────────────────────────────────────

@Composable
fun TargetVisualizationCanvas(
    shots: List<DetectedShot>,
    maxRings: Int,
    modifier: Modifier = Modifier
) {
    val ringColors = listOf(
        Color(0xFF263238), Color(0xFF37474F), Color(0xFF4E342E),
        Color(0xFFEF5350), Color(0xFFE53935), Color(0xFFEF9A9A),
        Color(0xFFFFD600), Color(0xFFFFEE58), Color(0xFFF9A825),
        Color(0xFF1B5E20)
    )

    Canvas(modifier = modifier.clipToBounds()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(size.width, size.height) / 2f * 0.92f

        // Scheibe zeichnen
        for (ring in 1..maxRings) {
            val radius = maxR * ring.toFloat() / maxRings
            val colorIndex = (maxRings - ring).coerceIn(0, ringColors.size - 1)
            drawCircle(color = ringColors[colorIndex], radius = radius, center = Offset(cx, cy))
            drawCircle(
                color = Color.White.copy(alpha = 0.25f), radius = radius,
                center = Offset(cx, cy), style = Stroke(width = 1.5f)
            )
        }

        // Fadenkreuz
        drawLine(Color.White.copy(alpha = 0.4f), Offset(cx, 0f), Offset(cx, size.height), 1f)
        drawLine(Color.White.copy(alpha = 0.4f), Offset(0f, cy), Offset(size.width, cy), 1f)

        // Einschläge
        shots.forEach { shot ->
            val x = shot.x * size.width
            val y = shot.y * size.height
            val r = maxR * 0.055f

            if (shot.isPatch) {
                // Pflaster
                drawRect(
                    color = Color(0xFFFAFAFA),
                    topLeft = Offset(x - r * 1.1f, y - r * 0.7f),
                    size = Size(r * 2.2f, r * 1.4f)
                )
                drawRect(
                    color = Color(0xFFBDBDBD),
                    topLeft = Offset(x - r * 1.1f, y - r * 0.7f),
                    size = Size(r * 2.2f, r * 1.4f),
                    style = Stroke(1.5f)
                )
            } else {
                val hitColor = when {
                    shot.confidence >= 0.85f -> Color(0xFF1565C0)
                    shot.confidence >= 0.60f -> Color(0xFFE65100)
                    else -> Color(0xFFB71C1C)
                }
                drawCircle(color = hitColor, radius = r, center = Offset(x, y))
                drawCircle(color = Color.White, radius = r, center = Offset(x, y), style = Stroke(2.5f))
                val arm = r * 1.8f
                drawLine(Color.White.copy(0.6f), Offset(x - arm, y), Offset(x + arm, y), 1f, StrokeCap.Round)
                drawLine(Color.White.copy(0.6f), Offset(x, y - arm), Offset(x, y + arm), 1f, StrokeCap.Round)
            }
        }

        // Ringbeschriftung
        for (ring in maxRings downTo maxOf(1, maxRings - 3)) {
            val radius = maxR * ring.toFloat() / maxRings
            drawContext.canvas.nativeCanvas.drawText(
                ring.toString(),
                cx + radius - 12f, cy - 4f,
                AndroidPaint().apply {
                    color = android.graphics.Color.WHITE; alpha = 150; textSize = 20f
                }
            )
        }
    }
}

// ────────────────────────────────────────────────
// ZOOMFÄHIGES FOTO
// ────────────────────────────────────────────────

@Composable
fun ZoomableImage(imagePath: String, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.clipToBounds().pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                if (scale > 1f) { offsetX += pan.x; offsetY += pan.y }
                else { offsetX = 0f; offsetY = 0f }
            }
        }
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = "Zielscheibe",
            modifier = Modifier.fillMaxSize().graphicsLayer(
                scaleX = scale, scaleY = scale,
                translationX = offsetX, translationY = offsetY
            ),
            contentScale = ContentScale.Fit
        )
        if (scale == 1f) {
            Text(
                "👆 Zum Zoomen",
                modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                    .background(Color.Black.copy(0.5f)).padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

// ────────────────────────────────────────────────
// SCHUSS-AUFSCHLÜSSELUNG
// ────────────────────────────────────────────────

@Composable
fun ShotBreakdownCard(shots: List<DetectedShot>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🔢 Erkannte Einschläge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("#", Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Ringe", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Typ", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Konfidenz", Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            shots.forEachIndexed { index, shot ->
                val confColor = when {
                    shot.confidence >= 0.85f -> Color(0xFF2E7D32)
                    shot.confidence >= 0.60f -> Color(0xFFF57F17)
                    else -> MaterialTheme.colorScheme.error
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", Modifier.width(28.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${shot.rings}", Modifier.width(56.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            shot.rings >= 9 -> Color(0xFF2E7D32)
                            shot.rings >= 7 -> Color(0xFFF9A825)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        if (shot.isPatch) "⬜ Pflaster" else "🎯 Treffer",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${(shot.confidence * 100).toInt()}%",
                        Modifier.width(80.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = confColor, textAlign = TextAlign.End, fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gesamt", fontWeight = FontWeight.Bold)
                Text("${shots.sumOf { it.rings }} Ringe",
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ────────────────────────────────────────────────
// TIPPS je nach Trefferzone
// ────────────────────────────────────────────────

@Composable
fun TipsCard(zone: ShotZone) {
    val (title, tips) = when (zone) {
        ShotZone.BULL -> "⭐ Exzellentes Trefferbild!" to listOf(
            "Anschlag und Abzug sind perfekt aufeinander abgestimmt.",
            "Kontinuierlich trainieren, um diese Leistung zu halten."
        )
        ShotZone.INNER -> "✅ Gutes Ergebnis" to listOf(
            "Leichte Streuung – prüfe deinen Nachschwung.",
            "Fokus auf gleichmäßigen Abzug ohne Reißen."
        )
        ShotZone.MIDDLE -> "📈 Ausbaufähig" to listOf(
            "Trefferbild gestreut – übe ruhigere Atmung.",
            "Trockenübungen für sauberen Abzug empfohlen.",
            "Diopter-Einstellung und Korn-Ausrichtung prüfen."
        )
        ShotZone.OUTER -> "⚠️ Großer Streukreis" to listOf(
            "Anschlagstabilität verbessern.",
            "Auflage oder Körperspannung prüfen.",
            "Trainer für Technik-Analyse konsultieren."
        )
        ShotZone.MISS -> "❌ Fehlschüsse erkannt" to listOf(
            "Visierung unbedingt prüfen.",
            "Mögliche Ursache: falscher Halt oder starker Wind."
        )
        ShotZone.UNKNOWN -> "ℹ️ Hinweis" to listOf("Trefferbild konnte nicht eindeutig zugeordnet werden.")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (zone) {
                ShotZone.BULL, ShotZone.INNER -> MaterialTheme.colorScheme.tertiaryContainer
                ShotZone.MIDDLE -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            tips.forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("→", style = MaterialTheme.typography.bodySmall)
                    Text(tip, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────
// DETAIL-KARTE
// ────────────────────────────────────────────────

@Composable
fun AnalysisDetailCard(result: TargetAnalysisResult) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📋 Analyse-Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            AnalysisRow(Icons.Default.GpsFixed, "Ringe gesamt", "${result.totalRings}")
            AnalysisRow(Icons.Default.FormatListNumbered, "Schüsse erkannt", "${result.shotCount}")
            if (result.patchesDetected > 0) {
                AnalysisRow(Icons.Default.Healing, "Schusspflaster",
                    "${result.patchesDetected} Stk. = ${result.patchRings} Ringe", highlight = true)
            }
            result.groupCenter?.let {
                AnalysisRow(Icons.Default.MyLocation, "Trefferbild-Lage", it.name)
            }
            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("KI-Konfidenz", style = MaterialTheme.typography.bodySmall)
                    Text("${(result.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { result.confidence },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        result.confidence >= 0.85f -> Color(0xFF2E7D32)
                        result.confidence >= 0.60f -> Color(0xFFF57F17)
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            if (result.confidence < 0.70f) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(
                            "Niedrige Konfidenz – Ergebnis manuell prüfen! Bessere Beleuchtung und höhere Auflösung verbessern die Erkennungsrate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp),
            tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
