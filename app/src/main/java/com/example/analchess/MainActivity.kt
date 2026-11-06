package com.example.analchess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.analchess.data.ApiService
import com.example.analchess.data.GameItem
import com.example.analchess.data.PlayerProfile
import com.example.analchess.data.SelectedGameHolder
import com.example.analchess.ui.GameHistoryAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var rootMain: LinearLayout
    private lateinit var etSearchUsername: EditText
    private lateinit var btnSearch: FrameLayout
    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvWinrateValue: TextView
    private lateinit var tvRatingRapid: TextView
    private lateinit var tvRatingBullet: TextView
    private lateinit var tvRatingBlitz: TextView
    private lateinit var tvGameCount: TextView
    private lateinit var layoutSearchLoading: LinearLayout
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvEmptyGamesHint: TextView
    private lateinit var rvGames: RecyclerView

    private lateinit var gameAdapter: GameHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootMain = findViewById(R.id.rootMain)
        etSearchUsername = findViewById(R.id.etSearchUsername)
        btnSearch = findViewById(R.id.btnSearch)
        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        tvUsername = findViewById(R.id.tvUsername)
        tvWinrateValue = findViewById(R.id.tvWinrateValue)
        tvRatingRapid = findViewById(R.id.tvRatingRapid)
        tvRatingBullet = findViewById(R.id.tvRatingBullet)
        tvRatingBlitz = findViewById(R.id.tvRatingBlitz)
        tvGameCount = findViewById(R.id.tvGameCount)
        layoutSearchLoading = findViewById(R.id.layoutSearchLoading)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)
        tvEmptyGamesHint = findViewById(R.id.tvEmptyGamesHint)
        rvGames = findViewById(R.id.rvGames)

        // Apply system window insets so the search bar sits cleanly below the status bar
        val density = resources.displayMetrics.density
        val horizontalPad = (18 * density).toInt()
        val extraTopPad = (18 * density).toInt()
        val extraBotPad = (14 * density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(rootMain) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                horizontalPad,
                sysBars.top + extraTopPad,
                horizontalPad,
                sysBars.bottom + extraBotPad
            )
            insets
        }

        gameAdapter = GameHistoryAdapter(emptyList()) { selectedGame ->
            openAnalysisScreen(selectedGame)
        }
        rvGames.layoutManager = LinearLayoutManager(this)
        rvGames.adapter = gameAdapter

        // Leave fields empty before any user is searched
        clearProfileState()

        btnSearch.setOnClickListener {
            performUserSearch()
        }

        etSearchUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performUserSearch()
                true
            } else {
                false
            }
        }
    }

    private fun clearProfileState() {
        tvUsername.text = "-"
        tvWinrateValue.text = "-%"
        tvRatingRapid.text = "-"
        tvRatingBullet.text = "-"
        tvRatingBlitz.text = "-"
        tvGameCount.text = ""
        ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder)
        gameAdapter.updateData(emptyList())
        tvEmptyGamesHint.visibility = View.VISIBLE
        layoutSearchLoading.visibility = View.GONE
    }

    private fun performUserSearch() {
        val query = etSearchUsername.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Masukkan username Chess.com terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        tvEmptyGamesHint.visibility = View.GONE
        layoutSearchLoading.visibility = View.VISIBLE
        tvStatusMessage.text = "Mencari \"$query\" di Chess.com..."

        ApiService.fetchChessComUser(query) { isSuccess, profile, errorMessage ->
            layoutSearchLoading.visibility = View.GONE
            if (isSuccess && profile != null) {
                bindProfile(profile)
            } else {
                if (gameAdapter.itemCount == 0) {
                    tvEmptyGamesHint.visibility = View.VISIBLE
                }
                Toast.makeText(
                    this,
                    errorMessage ?: "Gagal mengambil data pengguna",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindProfile(profile: PlayerProfile) {
        tvUsername.text = profile.username
        tvWinrateValue.text = "${profile.winratePercent}%"
        tvRatingRapid.text = profile.rapidRating.toString()
        tvRatingBullet.text = profile.bulletRating.toString()
        tvRatingBlitz.text = profile.blitzRating.toString()
        tvGameCount.text = profile.totalGamesCount.toString()

        ApiService.loadImage(profile.avatarUrl, ivUserAvatar, R.drawable.ic_user_placeholder)
        gameAdapter.updateData(profile.games)

        if (profile.games.isEmpty()) {
            tvEmptyGamesHint.visibility = View.VISIBLE
            tvEmptyGamesHint.text = "Tidak ada riwayat permainan terbaru untuk ${profile.username}."
        } else {
            tvEmptyGamesHint.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (::gameAdapter.isInitialized) {
            gameAdapter.notifyDataSetChanged()
        }
    }

    private fun openAnalysisScreen(game: GameItem) {
        SelectedGameHolder.selectedGame = game
        val intent = Intent(this, AnalysisActivity::class.java)
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: etSearchUsername
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
