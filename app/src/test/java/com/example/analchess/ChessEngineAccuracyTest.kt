package com.example.analchess

import com.example.analchess.data.ApiService
import com.example.analchess.data.ChessEngine
import com.example.analchess.data.GameItem
import com.example.analchess.data.MoveQuality
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ChessEngineAccuracyTest {

    @Test
    fun testAll48RealGamesPgnParsingAndFullAnalysis() {
        val file = File("src/test/resources/sukiwir56_2026_11.json")
        val content = file.readText()
        val parts = content.split("\"pgn\":\"")
        println("Found ${parts.size - 1} PGNs in archive")

        var mismatchCount = 0
        for (idx in 1 until parts.size) {
            val part = parts[idx]
            val endIdx = part.indexOf("\",\"time_control\"")
            if (endIdx < 0) continue
            val rawPgn = part.substring(0, endIdx)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            val curPosMatch = Regex("""\[CurrentPosition "([^"]+)"\]""").find(rawPgn)
            val expectedBoardFen = curPosMatch?.groupValues?.get(1)?.substringBefore(" ") ?: continue

            val states = ChessEngine.parsePgnToBoardStates(rawPgn)
            val actualBoardFen = states.last().fen.substringBefore(" ")
            if (expectedBoardFen != actualBoardFen) {
                mismatchCount++
                println("MISMATCH in game $idx: expected=$expectedBoardFen, actual=$actualBoardFen")
            }
        }
        assertEquals("All 48 real games must parse to exact CurrentPosition board", 0, mismatchCount)

        // Now test full move classification on Sukiwir56 (817) vs its_sinn (817)
        val pgn = """
            [Event "Live Chess"]
            [White "Sukiwir56"]
            [Black "its_sinn"]
            [Result "1-0"]
            [CurrentPosition "r6r/pp1b1p1p/4kpq1/1B1pp3/1Q1N4/8/P4PPP/4RRK1 b - - 9 21"]

            1. e4 {[%clk 0:30:00]} 1... c5 {[%clk 0:30:00]} 2. d4 {[%clk 0:29:56]} 2... cxd4 {[%clk 0:29:58.2]} 3. Qxd4 {[%clk 0:29:53.9]} 3... Nc6 {[%clk 0:29:58]} 4. Qd1 {[%clk 0:29:52.6]} 4... e5 {[%clk 0:29:53.7]} 5. Nf3 {[%clk 0:29:47.4]} 5... Qa5+ {[%clk 0:29:50]} 6. Bd2 {[%clk 0:29:42.1]} 6... Qb6 {[%clk 0:29:48.9]} 7. Nc3 {[%clk 0:29:27.9]} 7... Nf6 {[%clk 0:29:41]} 8. b3 {[%clk 0:28:52.5]} 8... Bb4 {[%clk 0:29:16.8]} 9. Nd5 {[%clk 0:27:48]} 9... Qc5 {[%clk 0:28:58.7]} 10. Nxf6+ {[%clk 0:26:48.7]} 10... gxf6 {[%clk 0:28:56.3]} 11. c3 {[%clk 0:26:34.3]} 11... Ba5 {[%clk 0:28:32.2]} 12. b4 {[%clk 0:26:05.6]} 12... Nxb4 {[%clk 0:28:30.2]} 13. cxb4 {[%clk 0:26:02.2]} 13... Bxb4 {[%clk 0:28:28.3]} 14. Bxb4 {[%clk 0:25:55.5]} 14... Qxb4+ {[%clk 0:28:27]} 15. Qd2 {[%clk 0:25:54.4]} 15... Qxe4+ {[%clk 0:28:20.6]} 16. Be2 {[%clk 0:25:46.2]} 16... d5 {[%clk 0:28:12.1]} 17. O-O {[%clk 0:25:43.2]} 17... Qg6 {[%clk 0:27:36]} 18. Bb5+ {[%clk 0:25:19.9]} 18... Ke7 {[%clk 0:27:32]} 19. Qb4+ {[%clk 0:25:14.4]} 19... Ke6 {[%clk 0:27:29.8]} 20. Rae1 {[%clk 0:24:33.4]} 20... Bd7 {[%clk 0:27:22.1]} 21. Nd4# {[%clk 0:24:21.4]} 1-0
        """.trimIndent()

        val states = ChessEngine.parsePgnToBoardStates(pgn)
        val tacticalResults = arrayOfNulls<ChessEngine.TacticalMoveAnalysis>(states.size)
        val onlineResults = arrayOfNulls<ApiService.OnlineStockfishEval>(states.size)
        for (i in 1 until states.size) {
            tacticalResults[i] = ChessEngine.analyzeMoveTactical(states[i - 1], states[i], isWhiteMove = (i % 2 == 1))
        }

        val gameItem = GameItem(
            id = "test",
            timeClass = "rapid",
            opponentName = "its_sinn",
            accuracyFormatted = "89,6",
            isPositive = true,
            whiteUsername = "Sukiwir56",
            whiteRating = 817,
            whiteAvatarUrl = null,
            blackUsername = "its_sinn",
            blackRating = 817,
            blackAvatarUrl = null,
            pgn = pgn,
            isUserWhite = true,
            officialWhiteAccuracy = 89.55,
            officialBlackAccuracy = 67.66
        )

        val summary = ApiService.buildSummaryFromResults(gameItem, states, tacticalResults, onlineResults)
        println("Summary: White(Sukiwir56) Acc=${summary.accuracy2}, Black(its_sinn) Acc=${summary.accuracy1}")
        println("Brilian: W=${summary.brilian2}, B=${summary.brilian1}")
        println("Bagus: W=${summary.bagus2}, B=${summary.bagus1}")
        println("Terbaik: W=${summary.terbaik2}, B=${summary.terbaik1}")
        println("SangatBaik: W=${summary.sangatBaik2}, B=${summary.sangatBaik1}")
        println("Buku: W=${summary.buku2}, B=${summary.buku1}")
        println("Kesalahan: W=${summary.kesalahan2}, B=${summary.kesalahan1}")
        println("Terlewat: W=${summary.terlewat2}, B=${summary.terlewat1}")
        println("Blunder: W=${summary.blunder2}, B=${summary.blunder1}")

        assertEquals(0, summary.brilian1)
        assertEquals(0, summary.brilian2)
        assertEquals(68, summary.accuracy1)
        assertEquals(90, summary.accuracy2)
        assertEquals(MoveQuality.TERBAIK, states[27].moveQuality) // 14. Bxb4
        assertEquals(MoveQuality.TERBAIK, states[28].moveQuality) // 14... Qxb4+
        assertEquals(MoveQuality.BLUNDER, states[40].moveQuality) // 20... Bd7??
        assertEquals(MoveQuality.TERBAIK, states[41].moveQuality) // 21. Nd4#
    }
}
