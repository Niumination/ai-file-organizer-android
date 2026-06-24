# Release Signing – AI File Organizer v1.1

## 1. Generate keystore

```bash
cd play-store/signing
bash generate-keystore.sh
# → ai-organizer-release.jks
# → upload_certificate.pem
```

Simpan `ai-organizer-release.jks` di:
- `~/.android/` (jangan commit)
- Backup ke 1Password / Bitwarden
- Upload base64 ke GitHub Secrets

## 2. keystore.properties (lokal)

Buat di ROOT project `keystore.properties`:
```
storeFile=/home/kamu/.android/ai-organizer-release.jks
storePassword=xxxx
keyAlias=aiorganizer
keyPassword=xxxx
```
Sudah di .gitignore.

## 3. GitHub Secrets

Settings → Secrets → Actions:
| Name | Value |
|---|---|
| SIGNING_KEYSTORE_BASE64 | `base64 -w 0 ai-organizer-release.jks` |
| SIGNING_STORE_PASSWORD | ... |
| SIGNING_KEY_ALIAS | aiorganizer |
| SIGNING_KEY_PASSWORD | ... |

## 4. Build

```bash
./gradlew bundleRelease   # AAB Play Store
./gradlew assembleRelease # APK
```

Output:
- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/release/app-release.apk`

Verify:
```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## 5. Play App Signing
- Upload AAB pertama → ini jadi upload key
- Google re-sign dengan app signing key mereka
- Simpan upload key baik-baik
