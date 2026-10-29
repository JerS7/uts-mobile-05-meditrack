# Penggunaan AI - MediTrack

Status: draf disclosure yang disusun 29 Oktober 2026 dari percakapan pengembangan dan riwayat Git lokal. Daftar ini perlu direview kedua anggota dan ditambah jika ada penggunaan AI di layanan atau percakapan lain.

AI digunakan untuk pemeriksaan kode, implementasi fitur, perbaikan, pengujian, dan penyusunan dokumentasi; cakupannya mencakup kode yang diadopsi langsung. Dokumen ini tidak membatasi peran AI menjadi sekadar saran.

Alat yang digunakan: OpenAI Codex dan Anthropic Claude. Berdasarkan konfirmasi anggota kelompok, Claude membantu membangun formulir pencatatan dan tombol Reset, sedangkan Codex menambahkan data dummy dan memperbaiki serta mengembangkan fitur. Rincian bantuan Codex dicatat dalam tabel berikut. Versi model dan tanggal sesi Claude belum dikonfirmasi.

## Catatan penggunaan

Tanggal fitur di bawah mengikuti commit terkait, **bukan timestamp prompt yang terverifikasi**. Prompt ditulis sebagai ringkasan, bukan transkrip verbatim. Tanggal/model sesi asli perlu dikonfirmasi dari riwayat percakapan sebelum pengumpulan.

| Tanggal rujukan | Alat AI | Ringkasan prompt | Kode/ide yang digunakan | Modifikasi dan pemahaman mahasiswa |
| --- | --- | --- | --- | --- |
| Belum dikonfirmasi | Claude | Bangun formulir pencatatan obat dan tombol Reset | Formulir pencatatan serta tombol untuk mengembalikan input ke kondisi awal | Peran Claude dikonfirmasi pengguna; prompt asli, tanggal, versi model, serta penjelasan modifikasi mahasiswa masih perlu dilengkapi |
| Belum terverifikasi | Codex | Periksa repo, clone lokal, perbaiki compile dan run | Bantuan penyiapan proyek dan diagnosis build | Detail perubahan awal serta penjelasan mahasiswa perlu dikonfirmasi |
| 2026-10-29, `05aa3e0` | Codex | Buat Scheduled, Taken, Missed menerima data; hapus data awal dan warning | Ringkasan mengikuti catatan dan alur input | Pengguna melaporkan pengujian berhasil; uraian review kode oleh mahasiswa belum dicatat |
| 2026-10-29, `9dd93d3` | Codex | Implementasikan database dan pencarian minimal 20 nama obat | Room catatan dan katalog terpisah berisi 30 nama, pencocokan nama | Pengguna meminta pemisahan katalog; pemahaman DAO, query, dan persistensi perlu ditulis mahasiswa |
| 2026-10-29, `fbec68b` | Codex | Tambahkan pengingat obat dan gabungkan ke Log Medication Record | Penyimpanan jadwal, alarm/notifikasi, kontrol pengingat dalam form | Integrasi form dilakukan sesuai revisi pengguna; penjelasan izin dan penjadwalan perlu dikonfirmasi mahasiswa |
| 2026-10-29, `9a154b5` | Codex | Tambahkan LazyColumn dan minimal tiga dummy; gunakan nama obat | LazyColumn, seed tiga catatan, pembaruan nama dummy, tes terkait | Nama direvisi atas permintaan pengguna menjadi Paracetamol, Ascorbic acid (Vitamin C), Loratadine; pemahaman key dan pencegahan duplikasi perlu ditulis mahasiswa |
| 2026-10-29 | Codex | Bandingkan proyek dengan PDF studi kasus 5 dan siapkan berkas sebelum commit, selain APK unduhan | Draf README, CONTRIBUTIONS, AI_USAGE, test-cases, checklist, bahan presentasi; pengelompokan aset lama | Belum direview mahasiswa; identitas, kontribusi, hasil manual, serta tanggal penggunaan AI harus dikonfirmasi |

## Verifikasi dan tanggung jawab

- Build dan tes yang dilakukan melalui agen dicatat sebagai pengujian berbantuan AI di [test-cases](test-cases.md), bukan pengujian manual mahasiswa.
- Kedua anggota perlu menjelaskan model, state, validasi, LazyColumn, rumus persentase, serta perbedaan data sementara dan Room.
- Isi kolom pemahaman dengan penjelasan sendiri setelah membaca dan mencoba kode. Jangan menandai review atau modifikasi mandiri selesai jika belum dilakukan.
- Bantuan Claude pada formulir dan Reset dicatat berdasarkan konfirmasi pengguna. Tambahkan penggunaan AI lain untuk desain/wireframe apabila ada.
