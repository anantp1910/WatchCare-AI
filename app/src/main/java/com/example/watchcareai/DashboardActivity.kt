package com.example.watchcareai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvPatientList: RecyclerView
    private lateinit var patientAdapter: PatientAdapter
    
    // Bottom navigation
    private lateinit var navHome: ImageView
    private lateinit var navStats: ImageView
    private lateinit var navAlerts: ImageView
    private lateinit var navSettings: ImageView

    private var alertListener: ListenerRegistration? = null
    private val patients = mutableListOf<Patient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        // Initialize views
        initializeViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadPatientData()
    }

    private fun initializeViews() {
        rvPatientList = findViewById(R.id.rvPatientList)
        navHome = findViewById(R.id.navHome)
        navStats = findViewById(R.id.navStats)
        navAlerts = findViewById(R.id.navAlerts)
        navSettings = findViewById(R.id.navSettings)
    }

    private fun setupRecyclerView() {
        // Add sample patient data (you can replace this with Firebase data)
        patients.addAll(
            listOf(
                Patient("101", "Mr. Johnson", "Sleeping", true),
                Patient("102", "Ms. Davis", "Active", true),
                Patient("105", "Mrs. Lee", "Irregular Heartbeat", false)
            )
        )

        patientAdapter = PatientAdapter(patients)
        rvPatientList.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = patientAdapter
        }
    }

    private fun setupBottomNavigation() {
        // Home is already selected (current screen)
        navHome.setColorFilter(resources.getColor(R.color.blue_400, theme))

        navHome.setOnClickListener {
            // Already on home
        }

        navStats.setOnClickListener {
            Toast.makeText(this, "Stats - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        navAlerts.setOnClickListener {
            Toast.makeText(this, "Alerts - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        navSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loadPatientData() {
        // Optional: Load patient data from Firebase
        // You can implement this to load real patient data from Firestore
        
        // Example: Listen to alerts collection (like the original MainActivity)
        alertListener = db.collection("alerts")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading alerts: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                // Handle alert updates here if needed
                snapshots?.documents?.let { docs ->
                    // You can update UI based on new alerts
                }
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        alertListener?.remove()
        super.onDestroy()
    }
}
