package com.example.analchess.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.analchess.R
import com.example.analchess.data.GameItem

class GameHistoryAdapter(
    private var items: List<GameItem>,
    private val onAnalyzeClick: (GameItem) -> Unit
) : RecyclerView.Adapter<GameHistoryAdapter.GameViewHolder>() {

    fun updateData(newItems: List<GameItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_history, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val item = items[position]

        val modeIconRes = when (item.timeClass.lowercase()) {
            "bullet" -> R.drawable.ic_bullet
            "blitz" -> R.drawable.ic_blitz
            else -> R.drawable.ic_rapid
        }
        holder.ivGameMode.setImageResource(modeIconRes)
        holder.tvOpponentName.text = item.opponentName
        holder.tvGameAccuracy.text = item.accuracyFormatted

        if (item.isPositive) {
            holder.tvResultBadge.text = "+"
            holder.tvResultBadge.setBackgroundResource(R.drawable.bg_badge_plus)
        } else {
            holder.tvResultBadge.text = "-"
            holder.tvResultBadge.setBackgroundResource(R.drawable.bg_badge_minus)
        }

        holder.btnAnalisa.setOnClickListener {
            onAnalyzeClick(item)
        }
        holder.itemView.setOnClickListener {
            onAnalyzeClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivGameMode: ImageView = itemView.findViewById(R.id.ivGameMode)
        val tvOpponentName: TextView = itemView.findViewById(R.id.tvOpponentName)
        val tvGameAccuracy: TextView = itemView.findViewById(R.id.tvGameAccuracy)
        val tvResultBadge: TextView = itemView.findViewById(R.id.tvResultBadge)
        val btnAnalisa: LinearLayout = itemView.findViewById(R.id.btnAnalisa)
    }
}

