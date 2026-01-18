package com.example.watchcareai

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val db = FirebaseFirestore.getInstance()

        db.collection("alerts")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("FIRESTORE", "alerts count = ${snapshot.size()}")
                for (doc in snapshot.documents) {
                    Log.d("FIRESTORE", "...")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "...", e)
            }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}