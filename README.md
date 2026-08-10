# AI File Organizer – Android

Aplikasi Android **merapikan file SD Card / internal storage pakai AI Gemini**, 100% SAF Scoped Storage, tidak pernah menyentuh file sistem.

- **Target:** Android 11+ / API 30+, compileSdk 34
- **AI:** Google Gemini (fallback chain: 2.5-flash → 2.0-flash → 2.0-flash-lite)
- **OCR on-device:** ML Kit Text Recognition (model auto-download saat install)
- **PDF parse:** PDFBox-Android
- **UI:** Jetpack Compose Material3 — tema visionOS *Liquid Glass*, edge-to-edge

## Fitur

- Pilih folder via `ACTION_OPEN_DOCUMENT_TREE` – app HANYA akses folder itu; pilihan tersimpan & tervalidasi ulang saat restart.
- Blacklist otomatis: `/Android`, `/Android/data`, `/Android/obb`, `.android_secure`, `MIUI`, `LOST.DIR`, folder tersembunyi **dan** `AI_Organized/` (hasil tidak discan ulang).
- Full Content Analysis:
  - TXT/MD/CSV/JSON → baca langsung
  - PDF → extract 2 halaman pertama
  - Gambar → OCR ML Kit (struk Tokopedia/Shopee, screenshot, KTP)
- Gemini batch 12 file → kategori + rename rapi, parsing JSON tahan markdown fence; gagal → fallback aturan ekstensi.
- Preview Plan → **Uji Coba (simulasi murni, tidak menulis apa pun)** → Eksekusi.
- Eksekusi aman-data: file sumber dihapus **hanya setelah** salinan terverifikasi byte-demi-byte; salinan setengah jadi otomatis dibersihkan.
- Scan/lari eksekusi bisa dibatalkan; tombol eksekusi terkunci selama proses (anti double-execution).
- API Key disimpan EncryptedSharedPreferences (dikecualikan dari backup & device transfer).

**Kategori:** Dokumen_Kerja, Dokumen_Pribadi, Struk_Invoice, Foto_Pribadi, Foto_Keluarga, Screenshot, Video, Audio_Musik, Ebook, Arsip_Project, APK_Installer, Download_Random, Lainnya

**Output:** `[FolderPilihan]/AI_Organized/[Kategori]/[Sub]/`

## Build Lokal

1. Android Studio Hedgehog+
2. Open folder
3. Sync Gradle
4. Run / Build APK

API Key gratis: https://aistudio.google.com/app/apikey

## Build via GitHub Actions

Push ke main → Actions otomatis build. Download APK di Artifacts / Releases (tag `v1.2.<run>`).

## Struktur

```
app/src/main/java/com/arena/aifileorganizer/
├── MainActivityVision.kt        — Entry, edge-to-edge, persist/restore SAF folder, Navigation (3 screens)
├── OrganizerViewModel.kt        — Scan → Extract → AI Categorize → Move (state machine, cancel-safe)
├── data/
│   ├── ApiKeyStore.kt           — EncryptedSharedPreferences (API key)
│   ├── SettingsStore.kt         — Persisted tree URI (non-secret)
│   └── GeminiClient.kt          — Gemini REST via OkHttp, model fallback chain, error manusiawi
├── model/
│   └── Models.kt                — Data classes + CategoryPresets
├── organizer/
│   ├── FileScanner.kt           — SAF recursive scan + blacklist + skip AI_Organized
│   ├── ContentExtractor.kt      — Text/PDF/OCR extractor (cancel-safe, closable)
│   ├── AiCategorizer.kt         — Gemini batch categorization + robust JSON parse + fallback
│   └── FileMover.kt             — Copy-verify-delete, dry-run tanpa tulis, cleanup parsial
└── ui/
    ├── Haptics.kt               — VisionHaptics (tick/success/warning)
    ├── VisionConfetti.kt        — Lottie confetti
    ├── theme/ThemeVision.kt     — Warna + tipografi Fraunces/Instrument Sans
    ├── theme/VisionModifiers.kt — liquidGlass, aurora, tilt, bob/breathe
    └── screens/
        ├── HomeScreenVision.kt    — API key (masked + toggle), folder picker, CTA
        ├── ScanScreenVision.kt    — Progress nyata + error retry + batalkan
        └── ResultScreenVision.kt  — Filter chips, plan cards, uji coba/eksekusi + confetti
```

MIT License
