package com.example.watchcareai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // Views
    private lateinit var tvNurseGreeting: TextView
    private lateinit var btnThemeToggle: TextView
    private lateinit var btnSettings: TextView
    private lateinit var noAlertsView: LinearLayout
    private lateinit var activeAlertView: LinearLayout
    private lateinit var tvAlertCount: TextView
    private lateinit var btnAcceptAlert: Button
    private lateinit var rvPastAlerts: RecyclerView
    
    private lateinit var pastAlertAdapter: PastAlertAdapter
    private var alertListener: ListenerRegistration? = null
    private var pastAlertsListener: ListenerRegistration? = null
    
    // Track alert IDs at startup
    private val startupAlertIds = mutableSetOf<String>()
    private var isFirstLoad = true
    
    // Current NEW alert IDs (only alerts that arrive AFTER login)
    private val newAlertIds = mutableSetOf<String>()
    
    // Notification setup
    private val channelId = "watchcare_channel"
    private val requestCode = 101
    private var notifId = 1
    
    private val sampleLocations = listOf(
        "Room 101", "Room 102", "Waiting Room", "ICU Room 4",
        "Lobby Cam 1", "Waiting Room Cam 3", "Hallway B2",
        "Emergency Room", "Ward 3 - Bed 5", "Pediatric Ward"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Already initialized
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        createNotificationChannel()
        initializeViews()
        setupNurseName()
        setupButtons()
        setupRecyclerView()
        
        // Start with 0 alerts
        updateAlertDisplay()
        
        startAlertsListener()
        loadPastAlerts()
    }

    private fun initializeViews() {
        tvNurseGreeting = findViewById(R.id.tvNurseGreeting)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        btnSettings = findViewById(R.id.btnSettings)
        noAlertsView = findViewById(R.id.noAlertsView)
        activeAlertView = findViewById(R.id.activeAlertView)
        tvAlertCount = findViewById(R.id.tvAlertCount)
        btnAcceptAlert = findViewById(R.id.btnAcceptAlert)
        rvPastAlerts = findViewById(R.id.rvPastAlerts)
    }

    private fun setupNurseName() {
        val userName = auth.currentUser?.displayName 
            ?: auth.currentUser?.email?.split("@")?.firstOrNull() 
            ?: "Nurse"
        tvNurseGreeting.text = "Hi, $userName"
    }

    private fun setupButtons() {
        btnThemeToggle.setOnClickListener {
            Toast.makeText(this, "Theme toggle - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnAcceptAlert.setOnClickListener {
            acceptCurrentAlert()
        }
    }

    private fun setupRecyclerView() {
        pastAlertAdapter = PastAlertAdapter()
        rvPastAlerts.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = pastAlertAdapter
        }
    }

    private fun startAlertsListener() {
        alertListener?.remove()

        Log.d("Dashboard", "Starting alerts listener...")

        // NO INDEX REQUIRED - Just listen to ALL alerts, filter in code
        alertListener = db.collection("alerts")
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Log.e("Dashboard", "Listener error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.e("Dashboard", "Snapshots is null")
                    return@addSnapshotListener
                }

                // Filter active alerts in code
                val activeAlerts = snapshots.documents.filter { doc ->
                    doc.getString("status") == "active"
                }
                
                val currentIds = activeAlerts.map { it.id }.toSet()
                Log.d("Dashboard", "Current active alerts: ${currentIds.size}")

                // FIRST LOAD - Record startup IDs
                if (isFirstLoad) {
                    startupAlertIds.addAll(currentIds)
                    isFirstLoad = false
                    Log.d("Dashboard", "First load - recorded ${startupAlertIds.size} startup alerts")
                    updateAlertDisplay()
                    return@addSnapshotListener
                }

                // SUBSEQUENT LOADS - Find truly NEW alerts
                val brandNewAlerts = currentIds - startupAlertIds - newAlertIds
                
                if (brandNewAlerts.isNotEmpty()) {
                    Log.d("Dashboard", "NEW ALERTS DETECTED: ${brandNewAlerts.size}")
                    
                    // Add to our new alerts tracking
                    newAlertIds.addAll(brandNewAlerts)
                    
                    // Update display
                    updateAlertDisplay()
                    
                    // Show toast
                    Toast.makeText(
                        this, 
                        "🚨 ${brandNewAlerts.size} New emergency detected!", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Show notification
                    if (ensureNotifPermission()) {
                        val newestDoc = activeAlerts.firstOrNull { it.id in brandNewAlerts }
                        val location = newestDoc?.getString("location") ?: "Unknown"
                        showNotification(
                            "🚨 WatchCare Emergency",
                            "New alert at: $location"
                        )
                    }
                } else {
                    Log.d("Dashboard", "No new alerts detected")
                }
                
                // Check if any alerts were removed (accepted elsewhere)
                val removedAlerts = newAlertIds - currentIds
                if (removedAlerts.isNotEmpty()) {
                    Log.d("Dashboard", "Alerts removed: ${removedAlerts.size}")
                    newAlertIds.removeAll(removedAlerts)
                    updateAlertDisplay()
                }
            }
    }

    private fun loadPastAlerts() {
        pastAlertsListener?.remove()
        
        // NO INDEX REQUIRED - Listen to all, filter in code
        pastAlertsListener = db.collection("alerts")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Dashboard", "Past alerts error: ${e.message}")
                    return@addSnapshotListener
                }
                
                if (snapshots == null) return@addSnapshotListener

                val pastAlerts = mutableListOf<PastAlert>()
                val currentUserName = auth.currentUser?.displayName 
                    ?: auth.currentUser?.email?.split("@")?.firstOrNull() 
                    ?: "You"
                
                snapshots.documents.forEach { doc ->
                    val status = doc.getString("status") ?: "active"
                    
                    // Filter in app - only claimed/resolved
                    if (status != "claimed" && status != "resolved") {
                        return@forEach
                    }
                    
                    val location = doc.getString("location") ?: sampleLocations.random()
                    val claimedBy = doc.getString("claimed_by") ?: "Unknown Nurse"
                    val claimedAt = doc.getLong("claimed_at") 
                        ?: doc.getTimestamp("created_at")?.toDate()?.time 
                        ?: System.currentTimeMillis()
                    
                    pastAlerts.add(
                        PastAlert(
                            id = doc.id,
                            location = location,
                            claimedBy = claimedBy,
                            timestamp = claimedAt,
                            isClaimedByMe = claimedBy.contains(currentUserName, ignoreCase = true)
                        )
                    )
                }

                // Sort by timestamp and take latest 20
                val sorted = pastAlerts.sortedByDescending { it.timestamp }.take(20)
                pastAlertAdapter.submitList(sorted)
            }
    }

    private fun updateAlertDisplay() {
        val count = newAlertIds.size
        Log.d("Dashboard", "Updating display - count: $count")
        
        if (count == 0) {
            noAlertsView.visibility = View.VISIBLE
            activeAlertView.visibility = View.GONE
        } else {
            noAlertsView.visibility = View.GONE
            activeAlertView.visibility = View.VISIBLE
            
            if (count == 1) {
                tvAlertCount.text = "Emergency detected"
            } else {
                tvAlertCount.text = "$count Emergencies detected"
            }
        }
    }

    private fun acceptCurrentAlert() {
        if (newAlertIds.isEmpty()) {
            Toast.makeText(this, "No new alerts to accept", Toast.LENGTH_SHORT).show()
            return
        }

        btnAcceptAlert.isEnabled = false
        
        // Get the first NEW alert
        val firstNewAlertId = newAlertIds.firstOrNull()
        if (firstNewAlertId == null) {
            btnAcceptAlert.isEnabled = true
            return
        }
        
        val userName = auth.currentUser?.displayName 
            ?: auth.currentUser?.email?.split("@")?.firstOrNull() 
            ?: "Nurse"
        
        Log.d("Dashboard", "Accepting alert: $firstNewAlertId")
        
        // Update to claimed
        db.collection("alerts").document(firstNewAlertId)
            .update(
                mapOf(
                    "status" to "claimed",
                    "claimed_by" to userName,
                    "claimed_at" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Log.d("Dashboard", "Alert accepted successfully")
                Toast.makeText(this, "✅ Alert accepted!", Toast.LENGTH_SHORT).show()
                
                // Remove from new alerts
                newAlertIds.remove(firstNewAlertId)
                updateAlertDisplay()
                
                btnAcceptAlert.isEnabled = true
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Failed to accept: ${e.message}")
                Toast.makeText(this, "❌ Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                btnAcceptAlert.isEnabled = true
            }
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
            val desc = "Emergency notifications from WatchCare AI"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
                enableVibration(true)
                enableLights(true)
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
            if (!granted) {
                Log.w("Dashboard", "Notification permission not granted")
                return
            }
        }

        Log.d("Dashboard", "Showing notification: $title - $body")

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        NotificationManagerCompat.from(this).notify(notifId++, notif)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        alertListener?.remove()
        pastAlertsListener?.remove()
        super.onDestroy()
    }
}
