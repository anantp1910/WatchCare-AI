package com.example.watchcareai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AlertAdapter
    private var alertListener: ListenerRegistration? = null

    private var lastSeenId: String? = null

    private val channelId = "watchcare_channel"
    private val requestCode = 101
    private val notifId = 1

    private val nurseId: String by lazy { getDeviceNurseId() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()

        createNotificationChannel()

        val recycler = findViewById<RecyclerView>(R.id.alertsRecycler)

        adapter = AlertAdapter { alert ->
            acceptAlert(alert.id)
        }

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        startAlertsListener()
    }

    private fun startAlertsListener() {
        alertListener?.remove()

        alertListener = db.collection("alerts")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots: QuerySnapshot?, e: FirebaseFirestoreException? ->

                if (e != null || snapshots == null) {
                    Toast.makeText(this, "Listener error: ${e?.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val alerts = snapshots.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "active"
                    val claimedBy = doc.getString("claimedBy")

                    val show = when (status) {
                        "active" -> true
                        "claimed" -> (claimedBy == nurseId)
                        else -> false
                    }
                    if (!show) return@mapNotNull null

                    val location = doc.getString("location") ?: "Unknown"
                    val severity = doc.getDouble("severity") ?: 0.0
                    val createdAt = doc.getTimestamp("created_at")?.toDate()?.time ?: 0L

                    Alert(
                        id = doc.id,
                        location = location,
                        severity = severity,
                        status = if (status == "claimed") "claimed by you" else status,
                        createdAtMillis = createdAt
                    )
                }

                adapter.submitList(alerts)

                val newestId: String? = snapshots.documents.firstOrNull()?.id

                if (lastSeenId == null) {
                    lastSeenId = newestId
                    return@addSnapshotListener
                }

                if (newestId != null && newestId != lastSeenId) {
                    val newestDoc = snapshots.documents.first()
                    val status = newestDoc.getString("status") ?: "active"

                    if (status == "active") {
                        val location = newestDoc.getString("location") ?: "Unknown"
                        val severity = newestDoc.getDouble("severity") ?: 0.0

                        Toast.makeText(this, "🚨 New alert received!", Toast.LENGTH_SHORT).show()

                        if (ensureNotifPermission()) {
                            showNotification(
                                "🚨 WatchCare Alert",
                                "Location: $location | Severity: ${"%.2f".format(severity)}"
                            )
                        }
                    }

                    lastSeenId = newestId
                }
            }
    }

    private fun acceptAlert(alertId: String) {
        val alertRef = db.collection("alerts").document(alertId)

        db.runTransaction { txn ->
            val snap = txn.get(alertRef)
            val status = snap.getString("status") ?: "active"

            if (status != "active") return@runTransaction false

            txn.update(alertRef, mapOf(
                "status" to "claimed",
                "claimedBy" to nurseId,
                "claimedAt" to FieldValue.serverTimestamp()
            ))
            true
        }.addOnSuccessListener { accepted ->
            if (accepted) {
                Toast.makeText(this, "✅ Accepted. Go to patient now.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Too late — another nurse accepted it.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { err ->
            Toast.makeText(this, "Accept failed: ${err.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeviceNurseId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_nurse"
    }

    private fun ensureNotifPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    requestCode
                )
                return false
            }
        }
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "WatchCare Alerts"
            val desc = "Notifications from WatchCare AI"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(notifId, notif)
    }

    override fun onDestroy() {
        alertListener?.remove()
        super.onDestroy()
    }
}
