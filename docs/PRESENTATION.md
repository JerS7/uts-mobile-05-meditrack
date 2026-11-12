# Bahan presentasi MediTrack

Draf untuk durasi 10-12 menit. Isi nama/NIM melalui CONTRIBUTIONS.md. Tentukan siapa yang menjelaskan tiap bagian setelah review bersama.

## 0-2 menit: masalah, pengguna, batasan

- Masalah: pengguna lupa apakah konsumsi obat sudah dicatat.
- Sasaran: pengguna dewasa dan pendamping yang memerlukan catatan.
- Solusi: catat nama, jadwal, waktu aktual, status, catatan; tampilkan rekap.
- Gunakan data simulasi. Pencarian hanya membantu memasukkan nama, tidak menentukan obat/dosis bagi pengguna.

## 2-6 menit: demonstrasi

1. Mulai dari instalasi baru: tunjukkan empty state di Beranda, lalu tekan Isi data contoh. Ringkasan hari ini menjadi 3 terdaftar, 2 Diminum, 1 Terlewat, 66%.
2. Buka Catat, coba simpan nama kosong untuk menunjukkan validasi.
3. Isi satu catatan Diminum dengan waktu aktual, simpan, lalu jelaskan perubahan menjadi 4/3/1 dan 75% di Beranda.
4. Pilih Terlewat dan tunjukkan Waktu Aktual dinonaktifkan; tunjukkan pemilih tanggal tidak mengizinkan hari esok.
5. Buka Riwayat: filter status, Ubah satu catatan, Hapus satu catatan lalu Urungkan.
6. Jika waktu cukup, buka Pengingat dan jelaskan tombol Sudah diminum/Terlewat pada notifikasi.

Jelaskan bahwa data disimpan dengan Room, sedangkan baseline UTS meminta data sementara. Jangan menjanjikan notifikasi muncul tepat saat demo sebelum diuji pada perangkat tersebut.

## 6-10/12 menit: defense individual dan perubahan langsung

Kedua anggota perlu mampu menjelaskan:

- Properti MedicationRecord, enum DoseStatus, serta actualTime yang boleh null.
- State form dan perubahan UI ketika input berubah.
- Alur validasi, simpan, penanganan kegagalan, dan reset setelah berhasil.
- Perbedaan state sementara dengan data Room yang bertahan antar sesi.
- LazyColumn, key berdasarkan ID, dan alasan setiap catatan menjadi item tersendiri.
- Rumus Taken/total, penanganan pembagian nol, serta tampilan persentase bilangan bulat.
- Katalog nama dan pengingat berada dalam database yang sama tetapi tabel terpisah; pengingat tidak dihitung sebagai catatan.
- Kolom tanggal, migrasi database versi 1 ke 2, serta alasan ID catatan dari notifikasi dibuat tetap per pengingat dan hari.
- Navigasi empat halaman dan alasan state form disimpan di tingkat aplikasi.
- Kontribusi pribadi, bantuan AI yang diadopsi, dan cara memverifikasi hasilnya.

Latihan perubahan: ubah batas panjang catatan, label tampilan, atau format persentase; jelaskan dampaknya dan uji ulang. Ini bahan latihan, bukan bukti bahwa defense atau pemahaman telah selesai.

## Persiapan sebelum presentasi

- Perbarui screenshot dan selesaikan minimal delapan skenario manual.
- Siapkan perangkat uji dengan data simulasi yang jumlahnya diketahui.
- Pastikan kedua anggota dapat menjalankan build dan menelusuri alur kode.
- Lengkapi identitas, kontribusi, disclosure AI, dan tautan APK final.
