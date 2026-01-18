package com.example.watchcareai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Get Started button click listener
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            // Navigate to Login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Optional: Add animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
