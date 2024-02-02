package com.example.dorstu

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PageCreeProfile : AppCompatActivity() {

    // Firestore
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_cree_profile)

        val profileButton = findViewById<Button>(R.id.btn_ajouter)
        profileButton.setOnClickListener {
            val profileName = findViewById<TextInputLayout>(R.id.textInputLayoutNomUtilisateur).editText?.text.toString()
            addProfileToFirestore(profileName)
        }
    }

    private fun addProfileToFirestore(profileName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileData = hashMapOf("username" to profileName,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .collection("profiles").add(profileData)
            .addOnSuccessListener {
                Log.d("Firestore", "Profile successfully written!")
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing profile", e)
            }
    }
}