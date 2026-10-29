# Skenario dan hasil pengujian

Disiapkan 29 Oktober 2026 untuk kode sampai commit `9a154b5`. Dokumen membedakan hasil otomatis yang tersedia, pemeriksaan kode, dan skenario manual yang masih menunggu pelaksanaan.

## Bukti tersedia

- Build `assembleDebug assembleDebugAndroidTest` berhasil pada 29 Oktober 2026 setelah perubahan nama dummy.
- Laporan XML lokal `app/build/test-results/testDebugUnitTest/` bertimestamp 29 Oktober 2026: 11 tes, 0 gagal, 0 error. Terdiri dari ExampleUnitTest (1), ReminderTimeTest (4), MedicationFormStateTest (1), MediTrackStateTest (5). Tes state sementara tidak membuktikan seluruh alur Room produksi.
- MedicationDatabaseTest: 2 tes lulus setelah perubahan nama dummy pada TECNO CM7 Android 16. Mencakup persistensi saat database dibuka kembali serta seed idempotent/pembaruan nama dummy dengan catatan lain tetap utuh.
- Pada perubahan LazyColumn sebelumnya, empty state dan pencarian nama lulus. Tes daftar sempat gagal karena Activity terhenti; pengulangan tes daftar secara terpisah lulus. Ini bukan bukti bahwa seluruh suite selalu stabil.
- Aplikasi hasil build terbaru berhasil dipasang dan dibuka pada perangkat. Seluruh pengujian di atas dijalankan melalui bantuan agen; penguji manual mahasiswa belum dicatat.

## Skenario manual

Untuk setiap baris, catat tanggal, nama penguji, hasil aktual, status lulus/gagal, dan bukti screenshot. Kolom yang belum dijalankan sengaja tidak ditandai lulus.

| ID | Langkah/input | Hasil yang diharapkan | Bukti/status saat disiapkan |
| --- | --- | --- | --- |
| TC-01 | Buka aplikasi pada instalasi baru tanpa data lama | Versi saat ini menampilkan 3 dummy, 2 Taken, 1 Missed, 66% | Seed diuji otomatis; verifikasi manual ringkasan belum dicatat. Berbeda dari empty state awal yang diminta PDF |
| TC-02 | Tampilkan MediTrackScreen dengan daftar kosong melalui tes UI | No records yet; ringkasan nol | Empty state lulus pada tes UI sebelumnya; tidak tersedia sebagai mode awal produksi saat seed aktif |
| TC-03 | Isi nama Paracetamol, jadwal 08:00 AM, aktual 08:05 AM, Taken, catatan simulasi; Add Record | Catatan muncul; total dan Taken bertambah 1; form kosong setelah simpan | Belum dicatat manual untuk versi akhir |
| TC-04 | Nama kosong atau spasi, waktu lain terisi; Add Record | Medication name is required; tidak menambah catatan | Ada aturan di kode; belum dicatat manual |
| TC-05 | Nama terisi, Scheduled Time belum dipilih; Add Record | Choose a scheduled time; tidak menambah catatan | Ada aturan di kode; belum dicatat manual |
| TC-06 | Pilih Taken tanpa Actual Time; Add Record | Choose the time you took it; tidak menambah catatan | Ada aturan di kode; belum dicatat manual |
| TC-07 | Isi waktu aktual lalu ganti status ke Missed; simpan dengan nama dan jadwal valid | Waktu aktual kosong/nonaktif, catatan Missed tersimpan tanpa waktu aktual | Ada aturan di kode; belum dicatat manual |
| TC-08 | Isi semua input lalu tekan Reset | Nama, waktu, catatan kosong; status Taken; pesan validasi hilang | Reset sesudah submit dicakup tes state; klik Reset di UI belum dicatat manual |
| TC-09 | Gulir daftar hingga semua tiga dummy terlihat | Nama dan status tiap catatan tampil di Card; jumlah 3 jika tidak ada catatan tambahan | Tes LazyColumn lulus setelah pengulangan sebelum koreksi nama; belum dicatat manual versi akhir |
| TC-10 | Dari tepat 3 dummy, tambah 1 Taken | Scheduled 4, Taken 3, Missed 1, persentase 75% | Ekspektasi berdasarkan rumus; belum dicatat manual |
| TC-11 | Ketik parasetamol, pilih Paracetamol, lalu ubah nama sendiri | Hasil pencarian dapat dipilih dan kolom tetap bisa diedit | Tes pencarian UI lulus pada perubahan sebelumnya |
| TC-12 | Tutup dan buka kembali aplikasi setelah simpan | Catatan bertahan, dummy tidak berulang | Persistensi dan idempotensi lulus otomatis; siklus tutup/buka manual belum dicatat |
| TC-13 | Isi nama/jadwal dan simpan pengingat; beri izin yang diminta | Pengingat tersimpan dengan nama/waktu yang dipilih; notifikasi muncul sesuai kondisi izin/perangkat | Pengiriman notifikasi versi akhir belum dicatat manual |
| TC-14 | Skenario huruf pada input angka, nilai negatif/nol | Tidak berlaku untuk kolom jumlah/dosis: aplikasi tidak memiliki kolom tersebut. Waktu dipilih lewat picker; 00:00 adalah waktu valid | N/A untuk kolom numerik. Jangan menganggap semua nol tidak valid |

## Format pencatatan pelaksanaan

| ID | Tanggal | Penguji | Perangkat/versi APK | Hasil aktual | Lulus/gagal | Bukti |
| --- | --- | --- | --- | --- | --- | --- |
| Isi setelah pengujian | | | | | | |

Untuk pengumpulan, jalankan dan dokumentasikan setidaknya delapan skenario yang relevan; jangan menghitung N/A sebagai tes lulus. Gunakan data simulasi. Tidak perlu menghapus data perangkat utama untuk menjalankan skenario instalasi baru; gunakan emulator/perangkat uji terpisah.

## Menjalankan tes otomatis

Unit test: `./gradlew.bat testDebugUnitTest`. Build APK dan test APK: `./gradlew.bat assembleDebug assembleDebugAndroidTest`. Tes perangkat dapat dijalankan melalui Android Studio pada kelas di `app/src/androidTest`. Siapkan perangkat uji dan izin yang sesuai. Laporan build di folder build tidak otomatis masuk Git.
