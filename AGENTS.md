# AGENT.md — Instruksi untuk AI Agent

> File ini berisi konteks, best practices, dan instruksi implementasi untuk AI agent yang mengerjakan repository ini.
> Dibuat berdasarkan audit autoskills v0.3.6 pada 2026-08-22.

---

## 📦 Tentang Project

**AI File Organizer** adalah aplikasi Android yang merapikan file di SD Card / internal storage menggunakan AI Google Gemini.

- **Bahasa:** Kotlin 1.9.22 (target upgrade ke 2.0+)
- **UI:** Jetpack Compose Material3 — tema visionOS "Liquid Glass"
- **AI:** Google Gemini API (fallback: 2.5-flash → 2.0-flash → 2.0-flash-lite)
- **OCR:** ML Kit Text Recognition (on-device)
- **PDF:** PDFBox-Android
- **Storage:** 100% SAF (Storage Access Framework) — scoped storage
- **Min SDK:** 30 (Android 11) | **Target/Compile SDK:** 34
- **Build:** Gradle 8.4, AGP 8.3.2, Compose BOM 2024.05.00

---

## 🗂️ Struktur Codebase

```
app/src/main/java/com/arena/aifileorganizer/
├── MainActivityVision.kt          # Entry point, edge-to-edge, SAF persist, Navigation
├── OrganizerViewModel.kt          # State machine: Scan → Extract → AI → Plan → Move
├── data/
│   ├── ApiKeyStore.kt             # EncryptedSharedPreferences (AES256-GCM)
│   ├── SettingsStore.kt           # Plain SharedPreferences (tree URI)
│   └── GeminiClient.kt            # OkHttp REST client + model fallback chain
├── model/
│   └── Models.kt                  # Data classes + CategoryPresets + Gemini DTOs
├── organizer/
│   ├── FileScanner.kt             # SAF recursive scan + blacklist
│   ├── ContentExtractor.kt        # Text/PDF/OCR extraction
│   ├── AiCategorizer.kt           # Gemini batch categorization + JSON parse + fallback
│   └── FileMover.kt              # Copy-verify-delete, dry-run, cleanup
└── ui/
    ├── Haptics.kt                 # VisionHaptics (vibration patterns)
    ├── VisionConfetti.kt          # Lottie confetti animation
    ├── theme/
    │   ├── ThemeVision.kt         # Colors + Typography (Fraunces + Instrument Sans)
    │   └── VisionModifiers.kt     # liquidGlass, aurora, tilt, bob/breathe
    └── screens/
        ├── HomeScreenVision.kt    # API key input + folder picker + CTA
        ├── ScanScreenVision.kt    # Progress + error retry + cancel
        └── ResultScreenVision.kt  # Filter chips + plan cards + uji coba/eksekusi
```

---

## 🎯 Best Practices yang Harus Diikuti

### Kotlin & Android
- Gunakan **sealed interfaces/classes** untuk state management (sudah ada: `ScanState`, `ExecuteState`, `Result`)
- Semua coroutine harus **scoped** (`viewModelScope`, `lifecycleScope`)
- Selalu handle **`CancellationException`** dengan `throw ce` setelah cleanup
- Gunakan **`runCatching {}`** untuk error handling yang aman
- Data classes untuk semua model/domain objects
- Extension functions untuk reusable UI modifiers
- `Dispatchers.IO` untuk disk/network, `Dispatchers.Default` untuk CPU-bound

### Jetpack Compose
- State hoisting — parameter masuk, event keluar (lambda)
- `collectAsState()` untuk Flow, `collectAsStateWithLifecycle()` jika di-Compose
- `remember` / `rememberSaveable` untuk state lokal
- Extract composable besar ke fungsi `@Composable private fun`
- Gunakan `Modifier` chain yang konsisten
- `LaunchedEffect` untuk side effects, `DisposableEffect` untuk cleanup

### Security
- **JANGAN** simpan secret di plain SharedPreferences — gunakan `EncryptedSharedPreferences`
- **JANGAN** hardcode API key — user input, encrypted at rest
- **JANGAN** allow backup untuk sensitive data
- **SELALU** gunakan HTTPS (`usesCleartextTraffic="false"`)
- **SELALU** validasi SAF permission sebelum akses file

### Data Safety (KRITIKAL — jangan pernah kompromi)
- **100% SAF** — jangan pernah gunakan direct file path access
- **Blacklist system folders** — Android/, .android_secure, MIUI, LOST.DIR
- **Copy-verify-delete** — jangan pernah delete source sebelum copy terverifikasi
- **Dry-run harus benar-benar tidak menulis** — simulation only
- **Cleanup partial copy** — hapus file tujuan jika copy gagal

### Arsitektur
- **Single Responsibility** — satu class, satu tanggung jawab
- **State machine** — gunakan sealed interface untuk state transitions
- **Graceful degradation** — AI gagal → fallback ke aturan ekstensi
- **Anti double-execution** — guard terhadap tap ganda
- **Stale data prevention** — sequence ID untuk scan/execute runs

---

## 🔧 Action Items dari Audit

### 🔴 P0 — Kritis

#### 1. Tambah Unit Tests
**Status:** Tidak ada test sama sekali  
**Target:** Minimal 60% coverage pada business logic

**Files yang harus dibuat:**
```
app/src/test/java/com/arena/aifileorganizer/
├── organizer/
│   ├── FileScannerTest.kt
│   ├── AiCategorizerTest.kt
│   ├── ContentExtractorTest.kt
│   └── FileMoverTest.kt
├── data/
│   └── GeminiClientTest.kt
└── OrganizerViewModelTest.kt
```

**Test dependencies yang diperlukan** (tambahkan ke `app/build.gradle.kts`):
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0")
```

**Test yang harus ada (minimal):**
- `FileScannerTest`: blacklist validation, max files limit, depth limit, skip AI_Organized
- `AiCategorizerTest`: JSON parse, fallback logic, batch chunking, illegal chars cleanup
- `GeminiClientTest`: model fallback chain, 429 retry, 401/403 abort, timeout handling
- `OrganizerViewModelTest`: state transitions, scan → done, cancel, double-execution guard
- `FileMoverTest`: dry-run no-write, copy-verify-delete, partial cleanup

**Instruksi implementasi:**
1. Tambah test dependencies ke `app/build.gradle.kts`
2. Buat test directory structure
3. Mulai dari `AiCategorizerTest` (paling mudah di-test, pure logic)
4. Lanjut ke `FileScannerTest` (perlu mock Context/DocumentFile)
5. Lalu `GeminiClientTest` (perlu mock OkHttpClient)
6. Terakhir `OrganizerViewModelTest` (integration test)

---

### ⚠️ P1 — Penting

#### 2. Pindahkan API Key ke Header
**File:** `app/src/main/java/com/arena/aifileorganizer/data/GeminiClient.kt`  
**Line:** ~72  
**Perubahan:**
```kotlin
// SEBELUM (API key di URL):
val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
val request = Request.Builder().url(url).post(requestBody).build()

// SESUDAH (API key di header):
val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
val request = Request.Builder()
    .url(url)
    .header("x-goog-api-key", apiKey)
    .post(requestBody)
    .build()
```

**Test:** Pastikan `GeminiClientTest` memverifikasi header ada di request.

---

#### 3. Hormati Dark Mode
**File:** `app/src/main/java/com/arena/aifileorganizer/MainActivityVision.kt`  
**Line:** ~60  
**Perubahan:**
```kotlin
// SEBELUM:
AIFileOrganizerThemeVision(darkTheme = false)

// SESUDAH:
AIFileOrganizerThemeVision(darkTheme = isSystemInDarkTheme())
```

**Import:** `import androidx.compose.foundation.isSystemInDarkTheme`

**Catatan:** Theme sudah support dark mode (ada `VisionDarkScheme`), hanya perlu aktivasi.

---

#### 4. Ganti Emoji dengan ImageVector + contentDescription
**Files:** Semua screen composables  
**Problem:** Emoji (`🔑`, `📂`, `🧪`, `⚡`, `📄`, `🖼️`, `🧾`, `✔️`, `🔒`, `🗂`) tidak accessible.

**Pendekatan:**
1. Buat file `app/src/main/java/com/arena/aifileorganizer/ui/icons/AppIcons.kt`
2. Definisikan icon menggunakan `Icons.Default.*` atau custom `ImageVector`
3. Ganti emoji di UI dengan `Icon()` + `contentDescription`

**Contoh:**
```kotlin
// SEBELUM:
Text("🔑  Gemini API Key", ...)

// SESUDAH:
Row {
    Icon(Icons.Default.Key, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Gemini API Key", ...)
}
```

**Mapping emoji → icon:**
- 🔑 → `Icons.Default.Key` atau `Icons.Default.VpnKey`
- 📂 → `Icons.Default.Folder`
- 🧪 → `Icons.Default.Science` (jika ada) atau custom
- ⚡ → `Icons.Default.Bolt` atau `Icons.Default.FlashOn`
- 🧾 → `Icons.Default.Receipt` atau custom

---

#### 5. Implementasi Hilt DI
**Status:** Semua dependency manual di-instantiate  
**Impact:** Sulit testing, tight coupling

**Instruksi implementasi:**
1. Tambah Hilt dependencies ke `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("dagger.hilt.android.plugin")
       id("kotlin-kapt")
   }
   dependencies {
       implementation("com.google.dagger:hilt-android:2.51")
       kapt("com.google.dagger:hilt-android-compiler:2.51")
   }
   ```
2. Buat `Application` class dengan `@HiltAndroidApp`
3. Buat `@Module` untuk `GeminiClient`, `FileScanner`, `ContentExtractor`, `FileMover`
4. Tambah `@HiltViewModel` + `@Inject constructor` di `OrganizerViewModel`
5. Tambah `@AndroidEntryPoint` di `MainActivityVision`

**Catatan:** Ini perubahan besar, sebaiknya di-branch terpisah dan review hati-hati.

---

#### 6. Upgrade Kotlin 2.0+
**File:** `build.gradle.kts` (root)  
**Perubahan:**
```kotlin
// SEBELUM:
id("org.jetbrains.kotlin.android") version "1.9.22" apply false
id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false

// SESUDAH:
id("org.jetbrains.kotlin.android") version "2.0.21" apply false
id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
```

**Compose compiler plugin** (Kotlin 2.0+):
```kotlin
// app/build.gradle.kts:
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    // HAPUS: composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    // Compose compiler sekarang otomatis via plugin
}
```

**Catatan:** Setelah upgrade, test build lokal dan CI sebelum merge.

---

#### 7. Upgrade compileSdk/targetSdk ke 35
**File:** `app/build.gradle.kts`  
**Perubahan:**
```kotlin
android {
    compileSdk = 35
    defaultConfig {
        targetSdk = 35
    }
}
```

**Catatan:** Cek [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-all) untuk breaking changes.

---

#### 8. Tambah Lint Check di CI
**File:** `.github/workflows/android.yml`  
**Tambah step:**
```yaml
- name: Run Lint
  run: ./gradlew lint --stacktrace

- name: Upload Lint Report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: lint-report
    path: app/build/reports/lint/
```

---

#### 9. Extract Fallback Logic
**Problem:** `fallbackDecision()` duplicate di `OrganizerViewModel` dan `AiCategorizer`  
**Solusi:** Buat utility class

**File baru:** `app/src/main/java/com/arena/aifileorganizer/organizer/FallbackCategorizer.kt`
```kotlin
object FallbackCategorizer {
    fun categorize(file: ScannedFile, reason: String = "fallback (aturan ekstensi)"): AiFileDecision {
        val ext = file.displayName.substringAfterLast('.', "").lowercase()
        val cat = when (ext) {
            "jpg", "jpeg", "png", "webp", "heic", "gif" -> "Foto_Pribadi"
            "mp4", "mkv", "mov", "3gp", "webm" -> "Video"
            // ... (sisanya sama)
            else -> "Download_Random"
        }
        return AiFileDecision(cat, null, file.displayName, 0.4f, reason)
    }
}
```

**Update:** `OrganizerViewModel.fallbackDecision()` dan `AiCategorizer.fallback()` untuk pakai utility ini.

---

#### 10. Handle Reduced-Motion Preference
**Files:** Semua screen composables dengan animasi  
**Instruksi:**
```kotlin
val animSpec = if (LocalDensity.current.fontScale > 1f) {
    // User prefers reduced motion
    tween(durationMillis = 0)
} else {
    // Normal animation
    infiniteRepeatable(tween(2600), RepeatMode.Reverse)
}
```

**Atau lebih simple:**
```kotlin
@Composable
fun reducedMotion(): Boolean {
    val density = LocalDensity.current
    return density.fontScale > 1.5f // proxy untuk reduced motion
}
```

**Animasi yang perlu di-handle:**
- `rememberVisionBob()` — floating animation
- `rememberVisionBreathe()` — scale animation
- `Spin` di `ScanScreenVision` — rotation animation
- `VisionConfetti` — Lottie animation

---

### 💡 P2 — Nice to Have

#### 11. Migrate ke Retrofit
**Impact:** Medium, tapi untuk single endpoint ini optional  
**Catatan:** Hanya lakukan jika ada plan menambah endpoint lain

#### 12. Gradle Version Catalog
**File baru:** `gradle/libs.versions.toml`  
**Instruksi:** Extract semua dependencies ke version catalog

#### 13. Update Compose BOM
**File:** `app/build.gradle.kts`  
**Perubahan:** `compose-bom:2024.05.00` → versi terbaru (cek [Compose BOM releases](https://developer.android.com/jetpack/compose/bom))

#### 14. Support Landscape/Tablet
**File:** `AndroidManifest.xml`  
**Perubahan:** Hapus `android:screenOrientation="portrait"`  
**Catatan:** Perlu test UI di landscape mode, mungkin perlu responsive layout

#### 15. Tambah OkHttp Logging Interceptor
**File:** `GeminiClient.kt`  
**Tambah:**
```kotlin
// Debug only:
if (BuildConfig.DEBUG) {
    client.addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
}
```

**Dependencies:** `implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")`

---

## 📋 Checklist Sebelum Submit PR

- [ ] Build berhasil: `./gradlew assembleDebug`
- [ ] Lint pass: `./gradlew lint`
- [ ] Test pass: `./gradlew test` (setelah ada test)
- [ ] Test di emulator/device — minimal API 30 dan API 34
- [ ] Review data safety — jangan kompromi SAF/copy-verify-delete
- [ ] Review security — jangan expose API key
- [ ] Update CHANGELOG.md jika ada perubahan user-facing
- [ ] Squash commits sebelum merge

---

## 🔄 Commit Convention

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat` — fitur baru
- `fix` — bug fix
- `refactor` — code change tanpa mengubah behavior
- `test` — tambah/update test
- `docs` — dokumentasi
- `chore` — build, CI, dependencies

**Scopes:**
- `auth` — API key handling
- `scanner` — file scanning
- `extractor` — content extraction
- `ai` — Gemini AI categorization
- `mover` — file moving
- `ui` — Jetpack Compose UI
- `theme` — visionOS theme
- `build` — Gradle/CI
- `test` — testing

**Contoh:**
```
feat(ai): add retry with exponential backoff for network errors

- Add retry mechanism for IOException in GeminiClient
- Implement exponential backoff: 1s, 2s, 4s
- Max 3 retries before fallback

Fixes #42
```

---

## 🚫 Anti-Patterns yang Harus Dihindari

1. **JANGAN** bypass SAF — selalu gunakan `DocumentFile` / `ContentResolver`
2. **JANGAN** delete file tanpa verifikasi copy
3. **JANGAN** store API key di plain text
4. **JANGAN** hardcode dark mode — selalu honor system preference
5. **JANGAN** gunakan emoji sebagai UI elements — gunakan `Icon` + `contentDescription`
6. **JANGAN** buat Composable > 300 baris — extract ke fungsi kecil
7. **JANGAN** instantiate dependency manual — gunakan DI (Hilt)
8. **JANGAN** ignore `CancellationException` — selalu `throw ce`
9. **JANGAN** blocking call di Main thread — selalu `Dispatchers.IO`
10. **JANGAN** commit tanpa test — minimal test business logic

---

## 📚 Resources

- [Android Architecture Guidelines](https://developer.android.com/topic/architecture)
- [Jetpack Compose Best Practices](https://developer.android.com/jetpack/compose/architecture)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android)
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [Gemini API Documentation](https://ai.google.dev/docs)

---

## 🤝 Catatan untuk Agent

Jika Anda adalah AI agent yang mengerjakan repository ini:

1. **Baca file ini terlebih dahulu** sebelum mulai coding
2. **Ikuti best practices** yang sudah didefinisikan di atas
3. **Kerjakan action items sesuai prioritas** (P0 → P1 → P2)
4. **Test setiap perubahan** — minimal manual test di emulator
5. **Jangan kompromi data safety** — ini nilai utama aplikasi ini
6. **Commit kecil dan fokus** — satu perubahan, satu commit
7. **Dokumentasikan keputusan** — jika ada trade-off, tulis di commit message

Jika ada pertanyaan atau ambiguity, **tanya user** daripada asumsi.

---

*File ini dibuat otomatis berdasarkan audit autoskills v0.3.6 pada 2026-08-22.*  
*Update file ini jika ada perubahan arsitektur atau best practices baru.*
