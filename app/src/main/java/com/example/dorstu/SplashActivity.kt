package com.example.dorstu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = Firebase.auth

        CoroutineScope(Dispatchers.IO).launch {
            val profilesLoaded = async { fetchProfiles() }
            profilesLoaded.await()

            withContext(Dispatchers.Main) {
                if (auth.currentUser != null) {
                    val intent = Intent(this@SplashActivity, ChoisirProfil::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this@SplashActivity, PageConnexion::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private suspend fun fetchProfiles(): Boolean {
        val userId = auth.currentUser?.uid ?: return false

        try {
            val documents = db.collection("users").document(userId)
                .collection("profiles")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!documents.isEmpty) {
                val profileName = documents.first().getString("username") ?: "Profile"
            }
            return true
        } catch (e: Exception) {
            Log.w("Firestore", "Error getting profiles", e)
            return false
        }
    }
}
