# SPESIFIKASI PROYEK: ANALCHESS (Aplikasi Analisis Catur Android)

> ⚠️ **PERHATIAN UNTUK AI BERIKUTNYA:**
> **JANGAN LANGSUNG EKSEKUSI / CODING TANPA INSTRUKSI DARI USER.**
> Dokumen ini adalah cetak biru (blueprint) dan panduan instruksi agar Anda memahami seluruh kebutuhan proyek, arsitektur, API, serta desain antarmuka sebelum mulai mengerjakan ketika user memberikan perintah jalan.

---

## 1. Ringkasan Proyek
**AnalChess** adalah aplikasi Android berbasis **Kotlin** untuk menganalisis permainan catur secara gratis dengan menghubungkan **Chess.com Public API** (untuk mencari username, profil, rating, dan riwayat permainan) serta memanfaatkan **Stockfish Online API** (gratis) untuk menganalisis langkah demi langkah, mengevaluasi papan catur, serta menghitung akurasi dan blunder.

---

## 2. Batasan Teknologi (Constraints)
Sesuai arahan eksplisit dari pemilik proyek:
1. **Bahasa Pemrograman**: **Kotlin murni** untuk Android.
2. **Framework / Arsitektur**:
   - **TIDAK menggunakan framework aneh-aneh** (Bukan Flutter, React Native, KMP/Kotlin Multiplatform, atau engine native C++ NDK yang berat).
   - Gunakan standar Android development: **Jetpack Compose** (direkomendasikan untuk UI modern dan fleksibel) atau Android Views (XML Standard).
   - Arsitektur: **MVVM (Model-View-ViewModel)** dengan Kotlin Coroutines & Flow.
   - Networking: **Retrofit 2** + OkHttp + Gson/Kotlinx Serialization.
   - Parser PGN & Aturan Catur: Library catur Kotlin/Java ringan (misalnya `ch.qos.logback` atau `com.github.bhlangonijr:chesslib` untuk validasi legal move dan FEN conversion) tanpa engine native C++ yang berat.

---

## 3. Referensi Desain Antarmuka (UI/UX)
Gambar desain acuan disimpan di root direktori proyek:
📁 `design_mockup.png`

Tema aplikasi menggunakan nuansa **Dark Theme** khas aplikasi catur (latar gelap `#1E1E1E` / `#262522`, aksen hijau catur `#81B64C`, teks putih `#FFFFFF` dan abu-abu `#A0A0A0`).

### Layar 1: Pencarian Pengguna & Riwayat Permainan (Screen Kiri di Mockup)
1. **Header / Search Bar**:
   - Input field: `"Cari Username"` dengan ikon pencarian (kaca pembesar) di sebelah kanan.
   - Ketika dicari, memanggil API Chess.com untuk mendapatkan profil & statistik user.
2. **Kartu Profil Pengguna**:
   - Foto profil / Avatar (placeholder jika tidak ada).
   - **Username** pemain.
   - **Winrate**: Contoh `"Winrate 63%"` dengan ikon grafik naik.
3. **Kartu Rating (3 Kategori Utama Chess.com)**:
   - ⏱️ **Cepat (Rapid)**: Ikon stopwatch/jam + nilai rating (contoh: 850).
   - 🚀 **Peluru (Bullet)**: Ikon peluru + nilai rating (contoh: 1201).
   - ⚡ **Kilat (Blitz)**: Ikon petir + nilai rating (contoh: 401).
4. **Daftar Riwayat Permainan ("permainan")**:
   - Menampilkan daftar match terakhir pemain.
   - Setiap baris (Game Card):
     - Ikon mode permainan (Cepat / Peluru / Kilat).
     - Nama lawan (`Nama_Lawan`).
     - Akurasi permainan (misal: `87,5` dengan badge hijau `+`, atau `42,3` dengan badge merah `-`).
     - Tombol Aksi: `[▶ Analisa]` (Tombol hijau). Jika ditekan, membuka **Layar 2** untuk menganalisis game tersebut.

### Layar 2: Papan Catur & Analisis Permainan (Screen Kanan di Mockup)
1. **Info Pemain Atas (Lawan / Hitam)**:
   - Avatar, Username & Rating (contoh: `drikkzz (635)`).
   - Bendera negara.
   - Bidak yang ditangkap (captured pieces) & indikator selisih perwira (contoh: `+10`).
2. **Papan Catur Interaktif (Chessboard)**:
   - Papan catur 8x8 (warna kotak hijau dan krem).
   - Bidak catur grafis standar (SVG/vektor).
   - **Eval Bar (Bar Evaluasi)** vertikal di sisi kanan papan yang menunjukkan keunggulan Putih/Hitam secara dinamis.
3. **Info Pemain Bawah (User / Putih)**:
   - Avatar, Username & Rating (contoh: `Sukiwir56 (812)`).
   - Bendera negara.
   - Bidak yang ditangkap (captured pieces).
4. **Kontrol Navigasi Langkah (Move Controller)**:
   - Tombol panah Sebelumnya (`<`) dan Selanjutnya (`>`).
   - Teks deretan notasi langkah: contoh `1. e4 d5  2. e5 e5` (dengan highlight pada langkah aktif saat ini).
5. **Tabel Breakdown Akurasi & Kualitas Langkah (Move Classification)**:
   - Header: Kolom Pemain Putih vs Hitam dengan ikon bidak pion.
   - **Akurasi**: Skor akurasi masing-masing pemain (0 - 100).
   - Klasifikasi Kategori Langkah (dengan jumlah kemunculan per pemain):
     - ❗ **Bagus** (Good move)
     - ⭐ **Terbaik** (Best move)
     - 👍 **Sangat Baik** (Excellent move)
     - ❓ **Kesalahan** (Inaccuracy / Mistake)
     - ❌ **Terlewat** (Missed win)
     - ❓❓ **Blunder** (Blunder)
   - Tombol panah ekspansi/collapse (`v`).
6. **Rating Permainan (Performance Rating)**:
   - Perkiraan rating kinerja di permainan ini (contoh: `150` vs `600`).

---

## 4. Integrasi API Eksternal

### A. Chess.com Public API (Gratis, Tanpa API Key)
Chess.com menyediakan REST API publik:
- **Header yang disyaratkan Chess.com**:
  ```http
  User-Agent: AnalChessApp/1.0 (contact: your_email_or_user)
  ```
- **Endpoint Profil Pengguna**:
  `GET https://api.chess.com/pub/player/{username}`
  - Mengambil `username`, `avatar`, `country`, `name`, dll.
- **Endpoint Statistik & Rating**:
  `GET https://api.chess.com/pub/player/{username}/stats`
  - Mengambil data rating dan win/loss/draw untuk `chess_rapid`, `chess_blitz`, `chess_bullet`.
  - Hitung Winrate: `(wins / total_games) * 100%`.
- **Endpoint Riwayat Permainan Pengguna**:
  `GET https://api.chess.com/pub/player/{username}/games/{year}/{month}`
  - Menghasilkan daftar objek game dalam format JSON yang berisi `pgn`, `white`, `black`, `time_class`, `url`, dll.

### B. Stockfish Engine Online (Gratis)
Untuk analisis tanpa membebani perangkat dan tanpa perlu kompilasi NDK C++:
1. **Stockfish Online API**:
   - `GET https://stockfish.online/api/s/v2.php?fen={fen}&depth=12`
   - Response JSON:
     ```json
     {
       "success": true,
       "evaluation": 0.45,
       "mate": null,
       "bestmove": "bestmove e2e4 ponder e7e5",
       "continuation": "e2e4 e7e5 g1f3"
     }
     ```
2. **Alternatif - Lichess Cloud Evaluation API**:
   - `GET https://lichess.org/api/cloud-eval?fen={fen}&multiPv=1`
   - Mengembalikan evaluasi centipawns / mate untuk posisi yang sudah terhitung di database Lichess.

### C. Logika Klasifikasi Langkah & Akurasi
Perbedaan evaluasi posisi sebelum langkah ($E_{before}$) dan sesudah langkah ($E_{after}$):
- $\Delta E = E_{best} - E_{played}$ (dari sudut pandang pemain yang melangkah)
  - **Terbaik (Best)**: $\Delta E \approx 0$ (atau langkah yang disarankan engine).
  - **Sangat Baik (Excellent)**: $\Delta E \le 0.20$ pawn loss.
  - **Bagus (Good)**: $\Delta E \le 0.50$ pawn loss.
  - **Kesalahan (Inaccuracy)**: $0.50 < \Delta E \le 1.00$ pawn loss.
  - **Terlewat (Missed Win)**: Kehilangan keuntungan menang drastis (misal dari $+4.0$ menjadi $0.0$).
  - **Blunder**: $\Delta E > 2.00$ pawn loss.
- **Perhitungan Akurasi**: Rumus akurasi berbasis probabilitas kemenangan (win probability drop) mirip Chess.com CAPS score:
  $$Accuracy = 100 \times \frac{1}{N}\sum f(\Delta WinPercent)$$

---

## 5. Rencana Langkah Implementasi (Saat Diberi Perintah Mulai)
Ketika user memerintahkan untuk mulai membuat proyek:
1. **Inisialisasi Project**:
   - Setup project Android standar (Gradle dengan Kotlin DSL).
   - Konfigurasi `build.gradle.kts` dengan dependensi resmi: Jetpack Compose / Material 3, Navigation, ViewModel, Retrofit, OkHttp, Coil (untuk gambar avatar).
   - Tambahkan library parser catur Java/Kotlin murni (`chesslib`).
2. **Data Layer & API Client**:
   - Buat Retrofit service untuk Chess.com API.
   - Buat client untuk Stockfish Online API.
   - Buat Repository & Data Models.
3. **Engine & Analisis Game Service**:
   - Parser PGN menjadi daftar FEN dan langkah.
   - Service coroutine untuk menganalisis FEN secara asinkron dengan Stockfish Online.
   - Kalkulator akurasi dan pengkategori langkah (Best, Good, Inaccuracy, Mistake, Blunder).
4. **UI Layer (Sesuai Desain)**:
   - Buat Komponen: BoardView, EvalBar, MoveListBar, AccuracyTable, PlayerHeaderCard.
   - Buat Screen 1: `UserSearchScreen` (Profil, Statistik, List Game).
   - Buat Screen 2: `GameAnalysisScreen` (Papan interaktif, controller move `<` `>`, tabel akurasi).
5. **Testing & Polishing**:
   - Uji coba dengan berbagai username Chess.com asli (contoh: `hikaru`, `magnuscarlsen`, atau username milik user).
   - Pastikan navigasi board lancar dan akurasi terhitung dengan benar.

---

*File ini dibuat otomatis untuk referensi sesi berikutnya atas instruksi pengguna.*

