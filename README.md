# Training Sheet

A simple, offline Android app for planning workouts and tracking strength progress. Build your own training days, fill them with exercises (sets, reps, weight), and see how your numbers change over time on a chart.

Black-and-white design, English interface, no account, no internet, no third-party libraries.

> Add screenshots here, e.g. `docs/screenshot-sheet.png`, `docs/screenshot-analysis.png`.

## Features

### Training sheet
- Create, rename and delete **training days** (e.g. Push, Pull, Day 1).
- Add **exercises** to each day with a name, description/notes, **sets**, **reps** and **weight (kg)**.
- Edit and delete exercises (with confirmation dialogs).
- Empty states for "no days yet" and "empty day".
- Everything is saved automatically and survives closing the app.

### Side menu (hamburger)
Open it with the three-line button next to the app title or by swiping from the left edge.

- **BMI Calculator** - height (cm) and weight (kg) give your BMI, category (underweight / normal / overweight / obesity), the normal weight range for your height, and a highlighted row in the category table.
- **Training Analysis** - history and progress chart (see below).

### Training Analysis
- Every time an exercise is saved, its sets, reps and weight are stored as a **history entry** with date and time.
- Pick any exercise (including ones that were deleted but still have history).
- **Line chart** with one point per saved entry, evenly spaced, with date and time under the axis. Tap or drag to see the exact value and timestamp.
- Switch the metric: **Weight**, **Reps**, **Sets**, **Volume** (sets x reps x weight) or **Est. 1RM** (Epley formula).
- Summary card: latest values, first and newest result, change (absolute and %), best result, number of entries.
- Full history list (newest first). Long-press an entry to delete it.

## Tech stack

| Area | Choice |
|---|---|
| Language | Java |
| UI | Android Views (XML layouts), `DrawerLayout`, AppCompat |
| Training days and exercises | `SharedPreferences` + JSON |
| History | SQLite via `SQLiteOpenHelper` |
| Chart | custom `LineChartView` drawn on a `Canvas` |
| Navigation | separate activities (`MainActivity`, `BmiActivity`, `AnalysisActivity`) |
| Theme | forced light mode, black-and-white palette, edge-to-edge layout |

No external libraries are needed beyond the default AndroidX/Material dependencies of an Empty Views Activity project.

## Project structure

```
app/src/main/
|-- AndroidManifest.xml
|-- java/com/example/trainingapp/
|   |-- MainActivity.java        training sheet screen + side menu
|   |-- BmiActivity.java         BMI calculator screen
|   |-- AnalysisActivity.java    history, metrics, chart screen
|   |-- Day.java                 model: a training day
|   |-- Exercise.java            model: an exercise (has a stable UUID)
|   |-- HistoryEntry.java        model: one saved sets/reps/weight snapshot
|   |-- HistoryDbHelper.java     SQLite helper for the history table
|   |-- TrainingRepository.java  load/save days (JSON) and write history
|   `-- LineChartView.java       custom line chart view
`-- res/
    |-- layout/
    |   |-- activity_main.xml
    |   |-- activity_bmi.xml
    |   |-- activity_analysis.xml
    |   |-- item_day_chip.xml
    |   |-- item_exercise.xml
    |   `-- item_history.xml
    |-- drawable/
    |   |-- bg_card.xml, bg_input.xml, bg_chip.xml, bg_chip_selected.xml
    |   |-- bg_fab.xml, bg_button_primary.xml, bg_button_outline.xml
    |   `-- ic_menu.xml, ic_arrow_back.xml
    `-- values/
        |-- colors.xml           ts_* black-and-white palette
        `-- strings.xml          app name
```

## Getting started

1. Install the latest **Android Studio**.
2. Open the project folder (`File > Open`) and let Gradle sync.
3. Pick an emulator (Device Manager) or connect a phone with USB debugging enabled.
4. Press **Run**.

Make sure both extra screens are registered inside `<application>` in `AndroidManifest.xml`:

```xml
<activity android:name=".BmiActivity" android:exported="false" />
<activity android:name=".AnalysisActivity" android:exported="false" />
```

### Recommended SDK settings (`app/build.gradle(.kts)`)
- `minSdk = 24` (Android 7.0) or higher
- `compileSdk = 36` and `targetSdk = 36` (required by Google Play for new apps and updates)

## How the data works

- **Days and exercises** are stored as one JSON string in `SharedPreferences` (`training_prefs`, key `data`).
- Each exercise has a stable `id` (UUID), so its history stays linked after renaming or reordering.
- **History** is stored in the SQLite database `training_history.db`, table `history`:

| column | meaning |
|---|---|
| `_id` | entry number |
| `exercise_id` | which exercise the entry belongs to |
| `exercise_name` | exercise name at the time of saving |
| `date_ms` | timestamp in milliseconds |
| `sets`, `reps`, `weight` | the saved values |

- Saving an exercise (new or edited) always adds one history entry, so you can also record "same weight again" and see a flat line.
- Deleting an exercise or day does **not** delete its history. It stays available in Training Analysis, marked "(deleted)", until you remove entries manually.
- **Migration:** data saved before history existed gets IDs and a first history entry automatically on first launch.

## Limitations

- Each exercise stores one set of current values (for example 4 x 8 x 80 kg), not every individual set of a workout. The chart therefore reflects the values you save over time.
- Data lives only on the device. Uninstalling the app removes it (Android backup may restore it on a new phone).
- No export/import yet.

## Roadmap

- Round Timer (boxing/MMA rounds, rest, warm-up)
- 1RM and plate calculators
- Rest timer between sets
- Settings (kg/lb, default rest time, clear data)
- Export / import backup (JSON or CSV)
- "Log workout" button that records a session without editing the plan
- Time range filter for the chart (30 days / 3 months / all)

## Publishing to Google Play (checklist)

- Change `applicationId` from `com.example.*` to your own (Google rejects `com.example`).
- Set `targetSdk` to 36 and update `versionCode` / `versionName`.
- Build a signed **App Bundle** (`Build > Generate Signed App Bundle`) and keep the keystore backed up.
- Prepare the store listing: icon 512x512, feature graphic 1024x500, at least 2 phone screenshots.
- Fill in the Play Console forms: privacy policy, Data safety, content rating, target audience.
- New personal developer accounts must run a closed test with at least 12 testers for 14 days before applying for production access.

## OpenSource

## Author

OuroDev
