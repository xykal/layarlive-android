# LayarLive — scene editor Android v0.2.0

App pendamping Android untuk TikTok LIVE **mobile game/screen-share**. Bukan aplikasi live pengganti TikTok dan tidak meminta login TikTok.

## Yang sudah ada

- Editor preview layar HP: pindahkan layer dengan drag dan ubah ukuran lewat handle sudut.
- Layer teks dengan ukuran dan beberapa preset warna; default ditempatkan di area atas.
- Layer Saweria Alert yang ditempel dari URL widget Saweria.
- URL widget disimpan terenkripsi dengan Android Keystore, dibatasi ke host Saweria HTTPS, dan tidak dikirim ke server LayarLive.
- Mode live terkunci dan **click-through**: elemen tampil tetapi sentuhan game diteruskan.
- Tombol notifikasi untuk mematikan overlay.

## Batasan

- Overlay Saweria memerlukan internet. Uji dulu pada LIVE percobaan—TikTok/Android bisa berbeda dalam menangkap overlay layar.
- LayarLive tidak menerima alert gift/follow TikTok secara otomatis; Saweria Alert adalah widget terpisah.
- LayarLive tidak memisahkan level suara game dan mic atau memfilter mic di dalam TikTok.
- URL widget Saweria dapat mengandung `streamKey`, jadi jangan kirim URL itu ke chat, issue publik, commit, atau workflow log. Masukkan URL langsung di app. Saweria menjelaskan overlay alert sebagai URL widget/browser source ([Saweria](https://about.saweria.co/)) dan mendukung custom HTML/CSS untuk overlay ([panduan](https://saweria.co/docs/custom-overlays)).

## Redmi A2

Spesifikasi resmi Xiaomi mencantumkan Android 12 (Go Edition), Micro-USB, dan jack headphone 3,5 mm ([spesifikasi Xiaomi](https://www.mi.com/global/product/redmi-a2/specs/)). Scene editor dibuat ringan, tetapi WebView Saweria bisa memakai RAM lebih banyak—kalau game/live terasa berat, matikan layer Saweria. Untuk mixer audio, pastikan aksesori cocok dengan Micro-USB/3,5 mm; jangan menganggap USB-C-only akan berfungsi.

## Build release melalui GitHub Actions

Workflow ada di `.github/workflows/android-release.yml`:

- `workflow_dispatch` meminta versi seperti `v0.2.1`, membangun APK release signed, lalu menerbitkannya sebagai GitHub Release.
- Push tag `v*` melakukan hal yang sama untuk tag tersebut.
- APK hanya disimpan sebagai GitHub Release asset; workflow **tidak mengunggah Actions artifact**.
- Jika repository publik, asset GitHub Release dapat diunduh publik. Hanya pemilik/collaborator dengan akses tulis atau token yang berizin repo yang dapat menjalankan workflow.

### Buat signing keystore sendiri

Keystore adalah kunci identitas aplikasi. Simpan cadangan offline di tempat aman; kunci yang sama diperlukan untuk menandatangani update berikutnya. Jangan commit file `.jks` atau kirim password/key ke chat.

Dengan JDK 17+, jalankan di komputer sendiri:

```bash
keytool -genkeypair -v \
  -keystore layarlive-release.jks \
  -alias layarlive \
  -keyalg RSA -keysize 2048 -validity 10000
```

Encode file keystore menjadi satu baris Base64:

- Linux: `base64 -w 0 layarlive-release.jks`
- macOS: `base64 -i layarlive-release.jks | tr -d '\n'`
- PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes('.\layarlive-release.jks'))`

Di GitHub repository, buka **Settings → Secrets and variables → Actions → New repository secret** lalu buat:

1. `ANDROID_KEYSTORE_BASE64` — hasil Base64.
2. `ANDROID_KEYSTORE_PASSWORD` — password keystore.
3. `ANDROID_KEY_ALIAS` — contoh `layarlive`.
4. `ANDROID_KEY_PASSWORD` — password key alias.

Setelah source project dan workflow berada di branch utama:

1. Jalankan workflow dari tab **Actions** untuk mendapat artifact APK.
2. Untuk link rilis publik, push tag versi, misalnya `v0.2.0`; workflow akan membuat GitHub Release.

> GitHub integration tidak tersedia di sesi ini, jadi file workflow sudah disiapkan tetapi belum bisa ku-push atau jalankan pada repo kamu. Upload source project ini ke repo GitHub milikmu dan masukkan secrets langsung di GitHub—jangan kirim kunci ke chat.

## Build lokal (opsional)

Dengan Android Studio, JDK 17, dan Android SDK Platform 35, jalankan `./gradlew assembleDebug` untuk uji lokal. Untuk `assembleRelease`, siapkan environment `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, dan `ANDROID_KEY_PASSWORD` dari keystore milikmu.
