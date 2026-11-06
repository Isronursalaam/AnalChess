# AnalChess - Aplikasi Analisis Catur Android (Kotlin)

Aplikasi Android murni (Kotlin + XML Views standar, tanpa framework pihak ketiga yang aneh-aneh) untuk menganalisis permainan catur secara gratis menggunakan **Stockfish Online API** dan terintegrasi langsung dengan **Chess.com Public API**.

## ✨ Fitur & Tampilan (Sesuai Desain `design_mockup.png`)

### 1. Layar Pencarian & Profil Pemain ([MainActivity.kt](app/src/main/java/com/example/analchess/MainActivity.kt))
- **Search Bar**: Input `"Cari Username"` berbentuk kapsul putih dengan tombol ikon kaca pembesar di kanan.
- **Kartu Profil**: Avatar pengguna, `Username`, dan `Winrate` (`63%`) lengkap dengan ikon grafik bar hijau.
- **Kartu Rating**:
  - **Cepat** (Rapid) dengan ikon timer hijau (`850`)
  - **Peluru** (Bullet) dengan ikon peluru emas (`1201`)
  - **Kilat** (Blitz) dengan ikon petir kuning (`401`)
- **Daftar Permainan (`permainan`)**:
  - Menampilkan ikon tipe permainan, nama lawan (`Nama_Lawan`), skor `Akurasi`, indikator menang/kalah (`+` hijau / `-` merah), dan tombol hijau **`▶ Analisa`**.

### 2. Layar Papan Catur & Analisis ([AnalysisActivity.kt](app/src/main/java/com/example/analchess/AnalysisActivity.kt))
- **Header Pemain Atas & Bawah**: Menampilkan avatar, username, rating (`drikkzz (635)` & `Sukiwir56 (812)`), bendera Indonesia, serta daftar perwira yang dimakan (`+10`).
- **Papan Catur 8x8 & Eval Bar ([ChessBoardView.kt](app/src/main/java/com/example/analchess/ui/ChessBoardView.kt))**:
  - Papan catur warna hijau-krem klasik (`#779556` & `#EBECD0`) dengan bidak catur bergaya Chess.com dan **Evaluation Bar** vertikal di sisi kanan papan.
- **Navigasi Langkah (`<` dan `>`)**:
  - Menampilkan notasi langkah (`1. e4 d5  2. e5 e5`) dengan garis bawah pada langkah aktif dan menggerakkan bidak serta Eval Bar secara interaktif.
- **Tabel Akurasi & Klasifikasi Langkah**:
  - Perbandingan 2 kolom pemain beserta ikon bidak pion.
  - Kotak **Akurasi** (putih di kiri, gelap di kanan).
  - Rincian 6 kualitas langkah: **Bagus** (`!`), **Terbaik** (`★`), **Sangat Baik** (`👍`), **Kesalahan** (`?`), **Terlewat** (`✕`), dan **Blunder** (`??`).
  - **Rating Permainan** di bagian bawah (`150` vs `600`).
