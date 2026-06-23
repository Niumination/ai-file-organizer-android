# AI File Organizer – Android

Aplikasi Android **merapikan file SD Card / internal storage pakai AI Gemini**, 100% SAF Scoped Storage, tidak pernah menyentuh file sistem.

- **Target:** Android 11+ / API 30+, compileSdk 34
- **AI:** Google Gemini 1.5 Flash
- **OCR on-device:** ML Kit Text Recognition
- **PDF parse:** PDFBox-Android
- **UI:** Jetpack Compose Material3

## Fitur

- Pilih folder via `ACTION_OPEN_DOCUMENT_TREE` – app HANYA akses folder itu.
- Blacklist otomatis: `/Android`, `/Android/data`, `/Android/obb`, `.android_secure`, `MIUI`, `LOST.DIR`
- Full Content Analysis:
  - TXT/MD/CSV/JSON → baca langsung
  - PDF → extract 2 halaman pertama
  - Gambar → OCR ML Kit (struk Tokopedia/Shopee, screenshot, KTP)
- Gemini batch 12 file → kategori + rename rapi
- Preview Plan → Dry-Run → Eksekusi
- API Key disimpan EncryptedSharedPreferences

**Kategori:** Dokumen_Kerja, Dokumen_Pribadi, Struk_Invoice, Foto_Pribadi, Foto_Keluarga, Screenshot, Video, Audio_Musik, Ebook, Arsip_Project, APK_Installer, Download_Random, Lainnya

**Output:** `[FolderPilihan]/AI_Organized/[Kategori]/[Sub]/`

## Build Lokal

1. Android Studio Hedgehog+
2. Open folder
3. Sync Gradle
4. Run / Build APK

API Key gratis: https://aistudio.google.com/app/apikey

## Build via GitHub Actions

Push ke main → Actions otomatis build. Download APK di Artifacts / Releases.

## Struktur

```
app/src/main/java/com/arena/aifileorganizer/
├── MainActivity.kt          — Entry + Navigation (3 screens)
├── OrganizerViewModel.kt    — Scan → Extract → AI Categorize → Move
├── data/
│   ├── ApiKeyStore.kt       — EncryptedSharedPreferences
│   └── GeminiClient.kt     — Gemini API via OkHttp
├── model/
│   └── Models.kt            — Data classes + CategoryPresets
├── organizer/
│   ├── FileScanner.kt       — SAF recursive scan
│   ├── ContentExtractor.kt  — Text/PDF/OCR extractor
│   ├── AiCategorizer.kt    — Gemini batch categorization
│   └── FileMover.kt        — Copy + delete (dry-run support)
└── ui/screens/
    ├── HomeScreen.kt        — API key + folder picker
    ├── ScanScreen.kt        — Progress indicator
    └── ResultScreen.kt      — Preview + dry-run/execute
```

MIT License
