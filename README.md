# 🎯 SchützenTracker

Eine vollständige Android-App in Kotlin zum Tracken von Schiesstraining im Schützenverein.

## Features

| Feature | Beschreibung |
|---|---|
| 📷 **KI-Scheibenanalyse** | Foto hochladen → Claude Vision erkennt Ringe & Schusspflaster automatisch |
| 📓 **Trainings-Tagebuch** | Alle Einheiten chronologisch mit Notizen, Datum, Disziplin |
| 📊 **Statistiken** | Fortschrittscharts, Durchschnitte, Trendanalyse |
| 🏆 **Bestleistungen** | Personal Bests je Disziplin automatisch erkannt |
| 🎯 **Multi-Disziplin** | Luftgewehr, Luftpistole, KK-Gewehr, Großkaliber, Bogen, uvm. |
| 🌤️ **Bedingungen** | Wind, Licht, Tageszeit, Müdigkeit → Korrelationsanalyse |
| 📤 **PDF-Export** | Trainingsberichte als PDF teilen |
| 🔔 **Erinnerungen** | Trainings-Erinnerungen per Benachrichtigung |
| 💡 **Streukreis-Analyse** | Treffergruppierung & Schwachstellenerkennung |
| 🏅 **Vereinsmodus** | Vergleich mit Vereins-Ø, Mannschaftsranglisten |

## Setup

### Voraussetzungen
- Android Studio Hedgehog oder neuer
- Android SDK 26+
- Anthropic API Key (für KI-Scheibenanalyse)

### Installation
```bash
git clone https://github.com/yourname/SchuetzenTracker
cd SchuetzenTracker
```

1. `local.properties` erstellen und API-Key eintragen:
```properties
ANTHROPIC_API_KEY=sk-ant-...
```

2. In Android Studio öffnen → Sync → Run

## Architektur

```
UI Layer         → Fragments + ViewModels (MVVM)
Domain Layer     → UseCases + Models
Data Layer       → Room DB + Retrofit + Repository
```

### Technologie-Stack
- **UI**: Jetpack Compose + Material Design 3
- **Navigation**: Navigation Component
- **Datenbank**: Room (SQLite)
- **Netzwerk**: Retrofit + OkHttp
- **KI**: Anthropic Claude Vision API
- **Charts**: MPAndroidChart
- **Kamera**: CameraX
- **DI**: Hilt
- **PDF**: iText7

## Disziplinen
- 🔫 Luftgewehr 10m (Auflage/Freihand)
- 🔫 Luftpistole 10m
- 🔫 KK-Gewehr 50m
- 🔫 KK-Pistole 25m
- 🔫 Großkaliber Gewehr/Pistole
- 🏹 Bogen (Recurve/Compound)
- ⬛ Benutzerdefiniert

## Scheibensystem
Ring-Erkennung unterstützt:
- **DSB-Scheiben** (Deutschen Schützenbund): Luftgewehr, KK, GK
- **ISSF-Scheiben** international
- **Bogenblätter** (FITA/WA)
- **Freie Konfiguration** (eigene Ringzahl definieren)
