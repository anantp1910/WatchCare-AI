package com.example.watchcareai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PastAlertAdapter : RecyclerView.Adapter<PastAlertAdapter.PastAlertViewHolder>() {

    private val alerts = mutableListOf<PastAlert>()

    fun submitList(newAlerts: List<PastAlert>) {
        alerts.clear()
        alerts.addAll(newAlerts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastAlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_past_alert, parent, false)
        return PastAlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: PastAlertViewHolder, position: Int) {
        holder.bind(alerts[position])
    }

    override fun getItemCount(): Int = alerts.size

    class PastAlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAlertTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        private val tvClaimedBy: TextView = itemView.findViewById(R.id.tvClaimedBy)
        private val tvAlertLocation: TextView = itemView.findViewById(R.id.tvAlertLocation)
        private val tvAlertTimestamp: TextView = itemView.findViewById(R.id.tvAlertTimestamp)

        fun bind(alert: PastAlert) {
            // Set title - always "Recent alert"
            tvAlertTitle.text = "Recent alert"

            // Set claimed by
            if (alert.isClaimedByMe) {
                tvClaimedBy.text = "Claimed by you"
            } else {
                tvClaimedBy.text = "Claimed by ${alert.claimedBy}"
            }

            // Set location
            tvAlertLocation.text = "Location: ${alert.location}"

            // Set timestamp (relative time)
            tvAlertTimestamp.text = getRelativeTime(alert.timestamp)
        }

        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
                hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}
