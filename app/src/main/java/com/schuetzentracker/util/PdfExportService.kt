package com.schuetzentracker.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.schuetzentracker.model.TrainingSession
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// ────────────────────────────────────────────────
// PDF EXPORT – nur Android SDK (kein iText nötig)
// android.graphics.pdf.PdfDocument ist seit API 19 verfügbar
// ────────────────────────────────────────────────

@Singleton
class PdfExportService @Inject constructor(
    private val context: Context
) {
    private val dtFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    // ── Paint-Objekte ───────────────────────────────────────────────

    private val titlePaint = Paint().apply {
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        color = Color.rgb(27, 94, 32) // Dunkelgrün
    }
    private val headingPaint = Paint().apply {
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        color = Color.rgb(50, 50, 50)
    }
    private val bodyPaint = Paint().apply {
        textSize = 12f
        color = Color.rgb(60, 60, 60)
    }
    private val smallPaint = Paint().apply {
        textSize = 10f
        color = Color.rgb(120, 120, 120)
    }
    private val accentPaint = Paint().apply {
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        color = Color.rgb(27, 94, 32)
    }
    private val linePaint = Paint().apply {
        color = Color.rgb(200, 230, 200)
        strokeWidth = 1f
    }

    // A4 Maße in Points (72 dpi)
    private val PAGE_WIDTH = 595
    private val PAGE_HEIGHT = 842
    private val MARGIN = 40f

    // ────────────────────────────────────────────────
    // EINZELNE SESSION → PDF
    // ────────────────────────────────────────────────

    fun exportSingleSession(session: TrainingSession): Uri {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        drawSessionPage(canvas, session)

        doc.finishPage(page)

        val file = File(
            context.cacheDir,
            "Training_${session.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))}.pdf"
        )
        doc.writeTo(file.outputStream())
        doc.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // ────────────────────────────────────────────────
    // MEHRERE SESSIONS → BERICHT
    // ────────────────────────────────────────────────

    fun exportSessions(sessions: List<TrainingSession>, title: String = "Trainingsbericht"): Uri {
        val doc = PdfDocument()
        var pageNum = 1

        // Titelseite
        val titlePageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum++).create()
        val titlePage = doc.startPage(titlePageInfo)
        drawTitlePage(titlePage.canvas, title, sessions)
        doc.finishPage(titlePage)

        // Seite pro Session (max 20 für Performance)
        sessions.take(20).forEach { session ->
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum++).create()
            val pg = doc.startPage(info)
            drawSessionPage(pg.canvas, session)
            doc.finishPage(pg)
        }

        val file = File(context.cacheDir, "Trainingsbericht_${System.currentTimeMillis()}.pdf")
        doc.writeTo(file.outputStream())
        doc.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // ────────────────────────────────────────────────
    // ZEICHENFUNKTIONEN
    // ────────────────────────────────────────────────

    private fun drawTitlePage(canvas: Canvas, title: String, sessions: List<TrainingSession>) {
        var y = 100f

        // Logo-Bereich (Zielscheibe als ASCII-Art)
        canvas.drawText("🎯", MARGIN, y, titlePaint.apply { textSize = 40f })
        titlePaint.textSize = 22f

        y += 60f
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 28f
        canvas.drawText(
            "Generiert am ${java.time.LocalDate.now().format(dateFormatter)}",
            MARGIN, y, smallPaint
        )
        y += 40f

        // Trennlinie
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 24f

        // Zusammenfassung
        canvas.drawText("Zusammenfassung", MARGIN, y, headingPaint)
        y += 22f

        val totalSessions = sessions.size
        val avg = sessions.map { it.totalRings }.average().takeIf { !it.isNaN() } ?: 0.0
        val pb = sessions.maxOfOrNull { it.totalRings }
        val competitions = sessions.count { it.isCompetition }

        listOf(
            "Trainingseinheiten gesamt:" to "$totalSessions",
            "Wettkämpfe:" to "$competitions",
            "Durchschnittliche Ringe:" to "${"%.1f".format(avg)}",
            "Persönlicher Beststand:" to "${pb ?: "-"} Ringe"
        ).forEach { (label, value) ->
            canvas.drawText(label, MARGIN, y, bodyPaint)
            canvas.drawText(value, MARGIN + 220f, y, accentPaint)
            y += 18f
        }

        y += 20f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 24f

        // Inhaltsverzeichnis
        canvas.drawText("Enthaltene Trainingseinheiten", MARGIN, y, headingPaint)
        y += 22f

        sessions.take(30).forEachIndexed { i, session ->
            canvas.drawText(
                "${i + 1}. ${session.date.format(dateFormatter)}  |  ${session.disciplineName}  |  ${session.totalRings} Ringe",
                MARGIN, y, bodyPaint
            )
            y += 16f
            if (y > PAGE_HEIGHT - 60f) return // Seite voll
        }

        drawFooter(canvas)
    }

    private fun drawSessionPage(canvas: Canvas, session: TrainingSession) {
        var y = MARGIN + 20f

        // ── Header ──
        canvas.drawText("🎯 Trainings-Protokoll", MARGIN, y, titlePaint)
        y += 30f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint.apply {
            color = Color.rgb(27, 94, 32); strokeWidth = 2f
        })
        y += 20f

        // ── Basis-Info ──
        canvas.drawText(session.disciplineName, MARGIN, y, headingPaint)
        if (session.isCompetition) {
            canvas.drawText("🏅 WETTKAMPF", MARGIN + 300f, y, accentPaint)
        }
        y += 20f
        canvas.drawText("Datum: ${session.date.format(dtFormatter)}", MARGIN, y, bodyPaint)
        y += 16f
        canvas.drawText("Ort: ${session.location}", MARGIN, y, bodyPaint)
        y += 30f

        // ── Gesamtergebnis ──
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint.apply {
            color = Color.rgb(200, 230, 200); strokeWidth = 1f
        })
        y += 20f
        canvas.drawText("Gesamtergebnis", MARGIN, y, headingPaint)
        y += 22f

        accentPaint.textSize = 26f
        canvas.drawText("${session.totalRings} Ringe", MARGIN, y, accentPaint)
        accentPaint.textSize = 13f
        canvas.drawText(
            "  (${session.series.size} Serien • ${session.totalShots} Schüsse)",  //
            MARGIN + 120f, y, smallPaint
        )
        y += 30f

        // ── Serien-Tabelle ──
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f
        canvas.drawText("Serien-Detail", MARGIN, y, headingPaint)
        y += 20f

        // Tabellen-Header
        canvas.drawText("Serie", MARGIN, y, smallPaint)
        canvas.drawText("Schüsse", MARGIN + 60f, y, smallPaint)
        canvas.drawText("Summe", MARGIN + 370f, y, smallPaint)
        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 12f

        session.series.forEach { series ->
            val shotsStr = series.shots.joinToString(" · ") { "${it.rings}" }
            canvas.drawText("${series.number}", MARGIN, y, bodyPaint)
            // Kürzen falls zu viele Schüsse
            val display = if (shotsStr.length > 55) shotsStr.take(52) + "…" else shotsStr
            canvas.drawText(display, MARGIN + 60f, y, bodyPaint)
            canvas.drawText("${series.totalRings}", MARGIN + 370f, y, accentPaint)
            y += 16f
        }

        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f

        // ── Bedingungen ──
        canvas.drawText("Bedingungen", MARGIN, y, headingPaint)
        y += 18f
        val cond = session.conditions
        canvas.drawText(
            "Wetter: ${cond.weather.label}   Wind: ${cond.wind.label}   Licht: ${cond.light.label}",
            MARGIN, y, bodyPaint
        )
        y += 16f
        canvas.drawText(
            "Ermüdung: ${cond.fatigue.label}   Stress: ${cond.stress.label}",
            MARGIN, y, bodyPaint
        )
        if (cond.equipment.isNotBlank()) {
            y += 16f
            canvas.drawText("Ausrüstung: ${cond.equipment}", MARGIN, y, bodyPaint)
        }

        // ── Notizen ──
        if (session.notes.isNotBlank()) {
            y += 28f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 18f
            canvas.drawText("Notizen", MARGIN, y, headingPaint)
            y += 18f
            // Zeilenumbruch bei langen Notizen
            val words = session.notes.split(" ")
            var line = ""
            words.forEach { word ->
                val test = if (line.isEmpty()) word else "$line $word"
                if (test.length > 72) {
                    canvas.drawText(line, MARGIN, y, bodyPaint)
                    y += 16f
                    line = word
                } else {
                    line = test
                }
            }
            if (line.isNotEmpty()) canvas.drawText(line, MARGIN, y, bodyPaint)
        }

        drawFooter(canvas)
    }

    private fun drawFooter(canvas: Canvas) {
        canvas.drawLine(
            MARGIN, PAGE_HEIGHT - 36f,
            PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 36f,
            linePaint
        )
        canvas.drawText(
            "SchützenTracker • Erstellt am ${java.time.LocalDate.now().format(dateFormatter)}",
            MARGIN, PAGE_HEIGHT - 20f, smallPaint
        )
    }

    // ────────────────────────────────────────────────
    // TEILEN
    // ────────────────────────────────────────────────

    fun shareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
