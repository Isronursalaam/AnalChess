package com.example.analchess.data

enum class MoveQuality {
    NONE,
    BRILIAN,     // Brilliant Move (Teal #1BACA6, '!!')
    BAGUS,       // Great / Good Move (Biru #5C8BB0, '!')
    TERBAIK,     // Best Move (Hijau #73B845, Bintang)
    SANGAT_BAIK, // Excellent Move (Hijau #81B64C, Jempol)
    BOOK,        // Book Move / Teori Pembukaan (Coklat #A88865, Buku)
    KESALAHAN,   // Mistake / Inaccuracy (Oranye #F09426, '?')
    TERLEWAT,    // Miss (Merah Oranye #E05347, 'X')
    BLUNDER      // Blunder (Merah #CA3431, '??')
}

data class PlayerProfile(
    val username: String,
    val avatarUrl: String?,
    val winratePercent: Int,
    val rapidRating: Int,
    val bulletRating: Int,
    val blitzRating: Int,
    val totalGamesCount: Int,
    val games: List<GameItem>
)

data class GameItem(
    val id: String,
    val timeClass: String, // "rapid", "bullet", "blitz"
    val opponentName: String,
    var accuracyFormatted: String, // e.g. "89,6"
    val isPositive: Boolean, // true -> green '+', false -> red '-'
    val whiteUsername: String,
    val whiteRating: Int,
    val whiteAvatarUrl: String?,
    val blackUsername: String,
    val blackRating: Int,
    val blackAvatarUrl: String?,
    val pgn: String,
    val isUserWhite: Boolean = true,
    val officialWhiteAccuracy: Double? = null,
    val officialBlackAccuracy: Double? = null,
    val isDefaultSample: Boolean = false
)

data class BoardState(
    val board: Array<CharArray>,
    val moveIndex: Int, // 0 = initial board, 1 = 1.e4, 2 = 1...d5, etc.
    val sanMove: String,
    val fen: String,
    val fromRow: Int = -1,
    val fromCol: Int = -1,
    val toRow: Int = -1,
    val toCol: Int = -1,
    var evalWhitePawns: Float = 0.0f, // 0.0 = 50/50 eval bar
    var moveQuality: MoveQuality = MoveQuality.NONE,
    val topCapturedText: String = "-",
    val bottomCapturedText: String = "-"
)

data class GameAnalysisSummary(
    val player1Name: String,
    val player2Name: String,
    var accuracy1: Int = 100,
    var accuracy2: Int = 100,
    var brilian1: Int = 0,
    var brilian2: Int = 0,
    var bagus1: Int = 0,
    var bagus2: Int = 0,
    var terbaik1: Int = 0,
    var terbaik2: Int = 0,
    var sangatBaik1: Int = 0,
    var sangatBaik2: Int = 0,
    var buku1: Int = 0,
    var buku2: Int = 0,
    var kesalahan1: Int = 0,
    var kesalahan2: Int = 0,
    var terlewat1: Int = 0,
    var terlewat2: Int = 0,
    var blunder1: Int = 0,
    var blunder2: Int = 0,
    var perfRating1: Int = 150,
    var perfRating2: Int = 600
)

object SelectedGameHolder {
    var selectedGame: GameItem? = null
}
