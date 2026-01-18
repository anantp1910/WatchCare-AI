package com.example.watchcareai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertAdapter : RecyclerView.Adapter<AlertAdapter.VH>() {

    private val items = mutableListOf<Alert>()

    fun submitList(newItems: List<Alert>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val t1 = itemView.findViewById<TextView>(android.R.id.text1)
        private val t2 = itemView.findViewById<TextView>(android.R.id.text2)

        fun bind(a: Alert) {
            t1.text = "${a.location} (${a.status})"
            t2.text = "Severity: ${"%.2f".format(a.severity)}"
        }
    }
}
