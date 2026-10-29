# Bahan presentasi MediTrack

Draf untuk durasi 10-12 menit. Isi nama/NIM melalui CONTRIBUTIONS.md. Tentukan siapa yang menjelaskan tiap bagian setelah review bersama.

## 0-2 menit: masalah, pengguna, batasan

- Masalah: pengguna lupa apakah konsumsi obat sudah dicatat.
- Sasaran: pengguna dewasa dan pendamping yang memerlukan catatan.
- Solusi: catat nama, jadwal, waktu aktual, status, catatan; tampilkan rekap.
- Gunakan data simulasi. Pencarian hanya membantu memasukkan nama, tidak menentukan obat/dosis bagi pengguna.

## 2-6 menit: demonstrasi

1. Tunjukkan tiga dummy dan ringkasan 3 jadwal, 2 Taken, 1 Missed, 66% pada perangkat uji yang hanya berisi dummy.
2. Coba simpan nama kosong untuk menunjukkan validasi.
3. Isi satu catatan Taken dengan waktu aktual, simpan, lalu jelaskan perubahan menjadi 4/3/1 dan 75%.
4. Pilih Missed dan tunjukkan Actual Time dinonaktifkan.
5. Tunjukkan Reset, gulir LazyColumn, dan jelaskan informasi tiap Card.
6. Jika waktu cukup, demonstrasikan pencarian nama dan penyimpanan pengingat dalam form.

Jelaskan bahwa versi ini telah memakai Room dan seed otomatis, sedangkan baseline UTS meminta data sementara dan empty state awal. Empty state komponen dapat ditunjukkan melalui pengujian UI. Jangan menjanjikan notifikasi muncul tepat saat demo sebelum diuji pada perangkat tersebut.

## 6-10/12 menit: defense individual dan perubahan langsung

Kedua anggota perlu mampu menjelaskan:

- Properti MedicationRecord, enum DoseStatus, serta actualTime yang boleh null.
- State form dan perubahan UI ketika input berubah.
- Alur validasi, simpan, penanganan kegagalan, dan reset setelah berhasil.
- Perbedaan state sementara dengan data Room yang bertahan antar sesi.
- LazyColumn, key berdasarkan ID, dan alasan setiap catatan menjadi item tersendiri.
- Rumus Taken/total, penanganan pembagian nol, serta tampilan persentase bilangan bulat.
- Katalog nama terpisah dari catatan; pengingat juga terpisah dari jumlah catatan.
- Kontribusi pribadi, bantuan AI yang diadopsi, dan cara memverifikasi hasilnya.

Latihan perubahan: ubah batas panjang catatan, label tampilan, atau format persentase; jelaskan dampaknya dan uji ulang. Ini bahan latihan, bukan bukti bahwa defense atau pemahaman telah selesai.

## Persiapan sebelum presentasi

- Perbarui screenshot dan selesaikan minimal delapan skenario manual.
- Siapkan perangkat uji dengan data simulasi yang jumlahnya diketahui.
- Pastikan kedua anggota dapat menjalankan build dan menelusuri alur kode.
- Lengkapi identitas, kontribusi, disclosure AI, dan tautan APK final.
