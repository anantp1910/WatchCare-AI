package com.example.watchcareai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView

class AlertAdapter(
    private val onAcceptClicked: (Alert) -> Unit
) : RecyclerView.Adapter<AlertAdapter.VH>() {

    private val items = mutableListOf<Alert>()

    fun submitList(newItems: List<Alert>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return VH(view, onAcceptClicked)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(
        itemView: View,
        private val onAcceptClicked: (Alert) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title = itemView.findViewById<TextView>(R.id.titleText)
        private val sub = itemView.findViewById<TextView>(R.id.subText)
        private val acceptBtn = itemView.findViewById<AppCompatButton>(R.id.acceptBtn)

        fun bind(a: Alert) {
            title.text = "${a.location} (${a.status})"
            sub.text = "Severity: ${"%.2f".format(a.severity)}"

            val canAccept = a.status == "active"
            acceptBtn.visibility = if (canAccept) View.VISIBLE else View.GONE

            acceptBtn.setOnClickListener { onAcceptClicked(a) }
        }
    }
}
