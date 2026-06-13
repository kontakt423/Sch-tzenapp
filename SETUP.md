# 📋 SETUP-ANLEITUNG – SchützenTracker

## Was du brauchst (Voraussetzungen)

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1+ | https://developer.android.com/studio |
| Java JDK | 11 oder 17 | Wird mit Android Studio mitgeliefert |
| Android SDK | API 26+ (Android 8.0) | Über Android Studio installierbar |
| Anthropic API-Key | – | https://console.anthropic.com |

---

## SCHRITT 1 – Projekt öffnen

1. ZIP entpacken (z.B. nach `~/AndroidStudioProjects/SchützenTracker`)
2. Android Studio starten
3. **File → Open** → Ordner `SchützenTracker` auswählen (den mit `build.gradle.kts`)
4. Android Studio lädt das Projekt und zeigt unten "Gradle sync" – warten

---

## SCHRITT 2 – API-Key eintragen

Im Projekt-Root (gleiche Ebene wie `build.gradle.kts`) eine neue Datei anlegen:

**Dateiname:** `local.properties`

```properties
# Android SDK Pfad – wird von Android Studio automatisch eingetragen
sdk.dir=/Users/DeinName/Library/Android/sdk

# Anthropic API Key – HIER deinen Key eintragen
ANTHROPIC_API_KEY=sk-ant-api03-DEIN-KEY-HIER
```

> ⚠️ Diese Datei ist in `.gitignore` eingetragen – sie wird nie ins Git-Repository hochgeladen.

**Wo bekomme ich den API-Key?**
1. Auf https://console.anthropic.com anmelden
2. Links im Menü: **API Keys** → **Create Key**
3. Den Key kopieren (er wird nur einmal angezeigt!)
4. In `local.properties` einfügen

---

## SCHRITT 3 – JitPack Repository hinzufügen

Für die Chart-Bibliothek (MPAndroidChart) musst du das JitPack-Repository hinzufügen.

**Datei: `settings.gradle.kts`** (neu anlegen im Projekt-Root):

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // ← DAS ist wichtig für Charts
    }
}

rootProject.name = "SchützenTracker"
include(":app")
```

---

## SCHRITT 4 – Gradle Sync & Build

1. In Android Studio: **File → Sync Project with Gradle Files**
   (oder das Elefanten-Symbol in der Toolbar)
2. Warten bis unten "BUILD SUCCESSFUL" erscheint (dauert beim ersten Mal 2–5 Min.)
3. Falls Fehler: Meist fehlt der `local.properties`-Key oder das JitPack-Repo

---

## SCHRITT 5 – App starten

### Auf echtem Android-Gerät (empfohlen wegen Kamera):
1. Auf dem Handy: **Einstellungen → Entwickleroptionen → USB-Debugging** aktivieren
2. Handy per USB verbinden
3. In Android Studio: Dein Gerät im Dropdown auswählen → ▶️ Run

### Auf Emulator (ohne Kamera-Test):
1. **Tools → Device Manager → Create Virtual Device**
2. Pixel 7 oder ähnliches auswählen → API 34
3. ▶️ Run

---

## SCHRITT 6 – Kamera-Berechtigung (Android 13+)

Beim ersten Start fragt die App automatisch nach Kamera-Zugriff. **Erlauben** klicken.

Falls die Berechtigung verweigert wurde:
- Android-Einstellungen → Apps → SchützenTracker → Berechtigungen → Kamera → Erlauben

---

## Häufige Fehler & Lösungen

### ❌ "ANTHROPIC_API_KEY is empty"
→ `local.properties` fehlt oder Key wurde nicht eingetragen. Schritt 2 wiederholen.

### ❌ "Could not resolve com.github.PhilJay:MPAndroidChart"
→ JitPack fehlt in `settings.gradle.kts`. Schritt 3 kontrollieren.

### ❌ "Unresolved reference: hiltViewModel"
→ Hilt-Plugin fehlt. In `build.gradle.kts` prüfen:
```kotlin
plugins {
    id("com.google.dagger.hilt.android")  // muss vorhanden sein
}
```

### ❌ "Room schema export" Warnung
→ Harmlos. Kann ignoriert werden oder mit dieser Zeile in `build.gradle.kts` beheben:
```kotlin
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}
```

### ❌ Kamera zeigt schwarzen Screen
→ App-Berechtigung prüfen (Schritt 6). Auf echtem Gerät testen, nicht Emulator.

---

## Projektstruktur (Übersicht)

```
SchützenTracker/
├── app/
│   ├── build.gradle.kts          ← Abhängigkeiten & API-Key-Einbindung
│   └── src/main/
│       ├── AndroidManifest.xml   ← Berechtigungen
│       └── java/com/schuetzentracker/
│           ├── api/              ← Claude Vision API
│           ├── data/
│           │   ├── database/     ← Room (SQLite)
│           │   └── repository/   ← Datenzugriff + DataStore
│           ├── di/               ← Hilt Dependency Injection
│           ├── model/            ← Daten-Modelle
│           ├── ui/
│           │   ├── analysis/     ← KI-Analyse Screen ← NEU
│           │   ├── camera/       ← CameraX Screen ← NEU
│           │   ├── diary/        ← Trainings-Tagebuch
│           │   ├── home/         ← Dashboard
│           │   ├── main/         ← Navigation
│           │   ├── profile/      ← Profil + Achievements
│           │   ├── settings/     ← Einstellungen (DataStore) ← AKTUALISIERT
│           │   ├── statistics/   ← Charts
│           │   ├── theme/        ← Material Design 3
│           │   └── training/     ← Neues Training erfassen
│           └── util/
│               ├── PdfExportService.kt
│               └── ReminderManager.kt
├── gradle/
│   └── libs.versions.toml        ← Alle Bibliothek-Versionen
├── build.gradle.kts              ← Root build config
├── settings.gradle.kts           ← JitPack + Repos ← WICHTIG
└── local.properties              ← API-Key (NICHT ins Git!)
```

---

## Was ist DataStore? (Einfach erklärt)

DataStore ist wie ein **dauerhaftes Notizbuch** für kleine Einstellungen:

```
Was Room speichert:          Was DataStore speichert:
─────────────────────        ────────────────────────
Trainingseinheiten           Dein Name
Serien                       Dein Vereinsname
Einzelschüsse                Standard-Disziplin
Ziele                        Erinnerung ein/aus
Achievements                 Uhrzeit der Erinnerung
                             API-Key (optional)
```

**Vorteil:** Einstellungen bleiben auch nach App-Neustart erhalten, ohne Datenbank-Overhead.

---

## Was kann CameraX? (Einfach erklärt)

CameraX ermöglicht **direktes Fotografieren aus der App** ohne Umweg über die Galerie:

```
Ohne CameraX:   App → System-Galerie öffnen → Foto auswählen → zurück
Mit CameraX:    App → direkt Kamera öffnen → Foto aufnehmen → sofort analysieren
```

**Features in der App:**
- Ausrichtungs-Hilfslinien (für gerades Foto der Scheibe)
- Blitz-Steuerung (Auto/An/Aus)
- Vorder-/Rückkamera wechseln
- Hohe Qualität für bessere KI-Erkennung
- Zoom per Pinch-Geste
