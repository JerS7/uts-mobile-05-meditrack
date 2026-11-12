# Skenario dan hasil pengujian

Diperbarui 12 November 2026 untuk versi dengan navigasi empat halaman, tanggal catatan, ubah/hapus, filter riwayat, tombol pada notifikasi, dan database gabungan (belum di-commit saat dokumen ini ditulis). Dokumen membedakan hasil otomatis yang tersedia, pemeriksaan kode, dan skenario manual yang masih menunggu pelaksanaan.

## Bukti tersedia

- 12 November 2026: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` berhasil. Unit test: 27 tes, 0 gagal, 0 error. Terdiri dari ExampleUnitTest (1), ReminderTimeTest (4), MediTrackStateTest (5), MedicationFormStateTest (9: validasi, tanggal, tanggal masa depan, ubah catatan, isian dari pengingat, konversi waktu), RecordStatsTest (3: ringkasan dan filter), DatesTest (5).
- 12 November 2026: SQL migrasi database versi 1 ke 2 disimulasikan dengan SQLite di komputer. Skema hasil migrasi sama dengan skema yang diharapkan Room, data catatan lama tetap ada dan diberi tanggal, nama dummy lama diganti, dan ID ganda tidak menyebabkan kegagalan. Ini bukan pengganti `MigrationTest` pada perangkat.
- 12 November 2026, TECNO CM7 Android 16: `connectedDebugAndroidTest` 15 tes, 0 gagal. MedicationDatabaseTest (4), MigrationTest (1), MedicineCatalogTest (1), ReminderDatabaseTest (1), MedicationListTest (4), MedicineSearchUiTest (1), EditDeleteUiTest (2), ExampleInstrumentedTest (1).
- 12 November 2026, TECNO CM7: versi ini dipasang di atas versi lama yang berisi 4 catatan dan 2 pengingat (TC-16). Keempat catatan tetap ada dengan tanggal hari itu, kedua pengingat tetap aktif dan terjadwal, berkas database lama terhapus.
- 12 November 2026, TECNO CM7, dijalankan agen melalui `adb` (bukan pengujian manual mahasiswa): TC-01, TC-02 (tanpa uji tekan ulang), TC-04 sampai TC-06 (pesan validasi), TC-09, TC-10, TC-12 (Hapus, Batal tidak dicoba di perangkat, Urungkan berhasil), TC-13 (filter Diminum), TC-14, dan penyimpanan pengingat pada TC-17 sesuai harapan. Screenshot ada di `docs/screenshots`. Alarm presisi untuk TC-17 terverifikasi terjadwal, tetapi pengujian dihentikan sebelum notifikasi muncul; TC-17 sampai TC-19 belum terbukti.
- Hasil versi sebelumnya (29 Oktober 2026, sampai commit `9a154b5`): dua tes MedicationDatabaseTest lulus pada TECNO CM7 Android 16; tes empty state, pencarian, dan LazyColumn sempat lulus. Hasil tersebut berlaku untuk kode lama.
- Seluruh pengujian di atas dijalankan melalui bantuan agen; penguji manual mahasiswa belum dicatat.

## Skenario manual

Untuk setiap baris, catat tanggal, nama penguji, hasil aktual, status lulus/gagal, dan bukti screenshot. Kolom yang belum dijalankan sengaja tidak ditandai lulus.

| ID | Langkah/input | Hasil yang diharapkan | Bukti/status saat disiapkan |
| --- | --- | --- | --- |
| TC-01 | Buka aplikasi pada instalasi baru tanpa data lama | Beranda menampilkan "Belum ada catatan", ringkasan 0 dan 0%, tombol Catat obat dan Isi data contoh | Dicakup tes UI (belum dijalankan); belum dicatat manual |
| TC-02 | Pada TC-01, tekan Isi data contoh | Tiga catatan hari ini: 3 terdaftar, 2 Diminum, 1 Terlewat, 66%. Menekan lagi tidak menambah duplikat | Idempotensi dicakup tes database (belum dijalankan); belum dicatat manual |
| TC-03 | Halaman Catat: Paracetamol, tanggal hari ini, jadwal 08:00 AM, aktual 08:05 AM, Diminum, catatan simulasi; Tambah Catatan | Pesan berhasil; form kosong; catatan muncul paling atas di Beranda dan Riwayat; ringkasan hari ini bertambah | Belum dicatat manual |
| TC-04 | Nama kosong atau spasi, kolom lain terisi; Tambah Catatan | "Nama obat harus diisi"; tidak menambah catatan | Dicakup unit test; belum dicatat manual |
| TC-05 | Nama terisi, Waktu Jadwal belum dipilih; Tambah Catatan | "Pilih waktu jadwal"; tidak menambah catatan | Dicakup unit test; belum dicatat manual |
| TC-06 | Pilih Diminum tanpa Waktu Aktual; Tambah Catatan | "Pilih waktu saat Anda meminumnya"; tidak menambah catatan | Dicakup unit test; belum dicatat manual |
| TC-07 | Isi waktu aktual lalu ganti status ke Terlewat; simpan dengan nama dan jadwal valid | Waktu aktual kosong/nonaktif, catatan Terlewat tersimpan tanpa waktu aktual | Dicakup unit test; belum dicatat manual |
| TC-08 | Isi semua input lalu tekan Reset | Nama, waktu, catatan kosong; tanggal hari ini; status Diminum; pesan validasi hilang | Dicakup unit test; belum dicatat manual |
| TC-09 | Pilih tanggal kemarin pada form lalu simpan | Catatan muncul di Riwayat pada grup "Kemarin", tidak dihitung di ringkasan Beranda hari ini | Belum dicatat manual |
| TC-10 | Buka pemilih tanggal pada form | Hari setelah hari ini tidak dapat dipilih | Belum dicatat manual |
| TC-11 | Riwayat: tekan Ubah pada sebuah catatan, ganti nama dan status, Simpan Perubahan | Kembali ke Riwayat; catatan yang sama berubah di posisinya, jumlah catatan tetap | Dicakup tes UI dan database (belum dijalankan); belum dicatat manual |
| TC-12 | Riwayat: tekan Hapus, lalu Batal; ulangi lalu konfirmasi Hapus; tekan Urungkan pada pesan | Batal tidak menghapus; Hapus menghilangkan catatan; Urungkan mengembalikannya | Konfirmasi dicakup tes UI (belum dijalankan); Urungkan belum diuji |
| TC-13 | Riwayat: filter Terlewat, lalu pilih satu tanggal, lalu Hapus filter | Hanya catatan yang cocok tampil; jumlah dan ringkasan mengikuti filter | Filter status dicakup unit dan tes UI (belum dijalankan) |
| TC-14 | Ketik parasetamol, pilih Paracetamol, lalu ubah nama sendiri | Saran dapat dipilih dan kolom tetap bisa diedit | Dicakup tes UI (belum dijalankan) |
| TC-15 | Tutup dan buka kembali aplikasi setelah simpan | Catatan bertahan dengan tanggalnya | Dicakup tes database (belum dijalankan); belum dicatat manual |
| TC-16 | Pasang versi ini di atas versi lama yang sudah berisi catatan dan pengingat | Catatan lama tetap ada dengan tanggal hari pembaruan; pengingat lama tetap ada dan aktif | Migrasi disimulasikan dengan SQLite; MigrationTest belum dijalankan; belum dicatat manual |
| TC-17 | Halaman Pengingat: isi nama dan waktu 1-2 menit ke depan, Simpan pengingat, beri izin notifikasi | Notifikasi "Pengingat Minum Obat" muncul dengan tombol Sudah diminum dan Terlewat | Belum diuji pada perangkat. Gunakan emulator atau mode senyap |
| TC-18 | Pada notifikasi TC-17 tekan Sudah diminum | Notifikasi hilang; catatan Diminum hari ini dengan jadwal pengingat dan waktu aktual saat itu muncul di Beranda | Belum diuji pada perangkat |
| TC-19 | Ulangi TC-17 lalu ketuk badan notifikasi | Aplikasi membuka halaman Catat dengan nama, jadwal, dan waktu aktual terisi | Belum diuji pada perangkat |
| TC-20 | Ubah pengingat yang sedang nonaktif lalu Perbarui pengingat | Pengingat tetap nonaktif | Diperiksa pada kode; belum dicatat manual |
| TC-21 | Saat berada di Riwayat atau Pengingat, tekan tombol Kembali | Kembali ke Beranda; dari Ubah Catatan kembali ke Riwayat | Belum dicatat manual |
| TC-22 | Skenario huruf pada input angka, nilai negatif/nol | Tidak berlaku untuk kolom jumlah/dosis: aplikasi tidak memiliki kolom tersebut. Waktu dipilih lewat picker; 00:00 adalah waktu valid | N/A untuk kolom numerik. Jangan menganggap semua nol tidak valid |

## Format pencatatan pelaksanaan

| ID | Tanggal | Penguji | Perangkat/versi APK | Hasil aktual | Lulus/gagal | Bukti |
| --- | --- | --- | --- | --- | --- | --- |
| Isi setelah pengujian | | | | | | |

Untuk pengumpulan, jalankan dan dokumentasikan setidaknya delapan skenario yang relevan; jangan menghitung N/A sebagai tes lulus. Gunakan data simulasi. Tidak perlu menghapus data perangkat utama untuk menjalankan skenario instalasi baru; gunakan emulator/perangkat uji terpisah. Untuk TC-17 sampai TC-19, notifikasi hanya berbunyi satu kali seperti notifikasi biasa (bukan alarm berulang); gunakan emulator atau mode senyap bila tidak ingin ada suara.

## Menjalankan tes otomatis

Unit test: `./gradlew.bat testDebugUnitTest`. Build APK dan test APK: `./gradlew.bat assembleDebug assembleDebugAndroidTest`. Tes perangkat: `./gradlew.bat connectedDebugAndroidTest` dengan perangkat atau emulator tersambung, atau melalui Android Studio pada kelas di `app/src/androidTest`. Laporan build di folder build tidak otomatis masuk Git.
