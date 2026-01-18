package com.example.watchcareai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Data class for Patient
data class Patient(
    val roomNumber: String = "",
    val name: String = "",
    val status: String = "",
    val isActive: Boolean = true
)

class PatientAdapter(
    private val patients: List<Patient>
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRoomNumber: TextView = itemView.findViewById(R.id.tvRoomNumber)
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvPatientStatus: TextView = itemView.findViewById(R.id.tvPatientStatus)
        val statusDot: View = itemView.findViewById(R.id.statusDot)

        fun bind(patient: Patient) {
            tvRoomNumber.text = patient.roomNumber
            tvPatientName.text = patient.name
            tvPatientStatus.text = patient.status

            // Set status dot color based on patient status
            val dotDrawable = when {
                patient.status.contains("irregular", ignoreCase = true) ||
                patient.status.contains("critical", ignoreCase = true) -> {
                    R.drawable.dot_yellow
                }
                patient.isActive -> R.drawable.dot_green
                else -> R.drawable.dot_yellow
            }
            statusDot.setBackgroundResource(dotDrawable)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }

    override fun getItemCount(): Int = patients.size
}
