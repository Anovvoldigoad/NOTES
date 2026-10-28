# Floating Notes untuk Android

Aplikasi catatan mengambang seperti catatan kecil di atas aplikasi lain. Catatan dapat dipindahkan dengan menyeret bilah judul, diubah ukurannya dari sudut kanan bawah, dan isi/posisi/ukurannya disimpan otomatis.

## Fitur

- Overlay menggunakan `TYPE_APPLICATION_OVERLAY`.
- Foreground service supaya overlay tetap aktif ketika pengguna berpindah aplikasi.
- Drag untuk memindahkan catatan.
- Resize handle di sudut kanan bawah.
- EditText multiline dengan scrollbar.
- Ketuk area catatan untuk meminta keyboard secara otomatis.
- `IME_FLAG_NO_EXTRACT_UI` menjaga editor tetap berada di overlay, sehingga keyboard tidak membuka mode layar penuh.
- Tombol notifikasi untuk menyembunyikan overlay.
- GitHub Actions untuk menghasilkan APK debug.

## Menjalankan di Android Studio

1. Buka folder proyek ini di Android Studio.
2. Tunggu Gradle sync selesai.
3. Jalankan `app` pada perangkat Android atau emulator.
4. Tekan **Buka izin tampil di atas aplikasi**, aktifkan izin untuk **Floating Notes**, lalu kembali ke aplikasi.
5. Tekan **Tampilkan catatan**.

Android memang mengatur bentuk keyboard (normal atau floating) melalui aplikasi keyboard yang digunakan pengguna. Kode ini meminta keyboard segera ketika area teks disentuh dan mencegah mode editor layar penuh; jika Gboard/keyboard yang digunakan mendukung floating keyboard, mode floating dapat dipilih dari kontrol keyboard tersebut.

## Build dari command line

```bash
./gradlew assembleDebug
```

APK akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Build dengan GitHub Actions

Workflow `.github/workflows/android.yml` berjalan pada push ke `main`/`master` dan pull request. Setelah selesai, buka artifact **floating-notes-debug** untuk mengunduh APK.

## Catatan distribusi

Aplikasi meminta izin `SYSTEM_ALERT_WINDOW`, notifikasi, dan foreground service khusus untuk menjaga overlay. Izin overlay harus diberikan manual oleh pengguna di pengaturan Android.
