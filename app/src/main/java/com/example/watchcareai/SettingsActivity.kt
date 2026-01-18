package com.example.watchcareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnEdit: TextView
    private lateinit var btnNotifications: LinearLayout
    private lateinit var btnPrivacy: LinearLayout
    private lateinit var switchDetection: SwitchCompat
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        btnEdit = findViewById(R.id.btnEdit)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnPrivacy = findViewById(R.id.btnPrivacy)
        switchDetection = findViewById(R.id.switchDetection)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupClickListeners() {
        // Edit profile button
        btnEdit.setOnClickListener {
            Toast.makeText(this, "Edit Profile - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Notifications setting
        btnNotifications.setOnClickListener {
            Toast.makeText(this, "Notification Settings - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Privacy & Security setting
        btnPrivacy.setOnClickListener {
            Toast.makeText(this, "Privacy & Security - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Detection sensitivity toggle
        switchDetection.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(this, "High accuracy mode $status", Toast.LENGTH_SHORT).show()
            
            // Here you can save the preference to SharedPreferences or Firebase
            // Example:
            // getSharedPreferences("settings", MODE_PRIVATE)
            //     .edit()
            //     .putBoolean("high_accuracy", isChecked)
            //     .apply()
        }

        // Logout button
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        // Sign out from Firebase
        auth.signOut()

        // Navigate back to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        // Go back to dashboard when back button is pressed
        super.onBackPressed()
        finish()
    }
}
