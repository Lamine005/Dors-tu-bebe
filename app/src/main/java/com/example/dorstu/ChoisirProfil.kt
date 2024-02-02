package com.example.dorstu

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.dorstu.databinding.ActivityChoisirProfilBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog


class ChoisirProfil : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityChoisirProfilBinding
    private val db = FirebaseFirestore.getInstance()
    private val profileButtons = arrayOfNulls<Button>(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChoisirProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.button5.setOnClickListener {
            val selectedProfileName = binding.button5.text.toString() // Get the profile name from the button text
            openPageHistoireWithProfile(selectedProfileName)
        }

        binding.button9.setOnClickListener {
            val intent = Intent(this, PageCreeProfile::class.java)
            startActivityForResult(intent, CREATE_PROFILE_REQUEST_CODE)
        }

        binding.btnDeconnexion.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, PageConnexion::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnParametres.setOnClickListener {
            showDeleteProfileDialog()
        }

        initializeProfileButtons()
        loadProfiles()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CREATE_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            fetchProfiles()
        }
    }

    private fun showDeleteProfileDialog() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profiles = mutableListOf<String>()
        val profileIds = mutableListOf<String>()

        db.collection("users").document(userId)
            .collection("profiles").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    profiles.add(document.getString("username") ?: "")
                    profileIds.add(document.id)
                }
                val arrayAdapter = ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, profiles)
                AlertDialog.Builder(this)
                    .setTitle("Sélectionnez un profil à supprimer")
                    .setAdapter(arrayAdapter) { dialog, which ->
                        deleteProfile(userId, profileIds[which])
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error loading profiles", e)
            }
    }

    private fun deleteProfile(userId: String, profileId: String) {
        db.collection("users").document(userId)
            .collection("profiles").document(profileId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Profile successfully deleted")
                Toast.makeText(this, "Profil supprimé", Toast.LENGTH_SHORT).show()
                loadProfiles() // Recharger la liste des profils
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting profile", e)
            }
    }


    private fun initializeProfileButtons() {
        profileButtons[0] = findViewById(R.id.button5)
        profileButtons[1] = findViewById(R.id.button6)
        profileButtons[2] = findViewById(R.id.button7)
        profileButtons[3] = findViewById(R.id.button8)

        // Définir des OnClickListener pour chaque bouton de profil
        profileButtons.forEachIndexed { index, button ->
            button?.setOnClickListener {
                val selectedProfileName = button.text.toString()
                openPageHistoireWithProfile(selectedProfileName)
            }
        }
    }

    private fun loadProfiles() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("profiles").get()
            .addOnSuccessListener { documents ->
                for ((index, document) in documents.withIndex()) {
                    profileButtons[index]?.apply {
                        text = document.getString("username")
                        visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error loading profiles", e)
            }
    }

    private fun openPageHistoireWithProfile(profileName: String) {
        val intent = Intent(this, PageHistoire::class.java)
        intent.putExtra("profileName", profileName)
        intent.putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
        startActivity(intent)
    }

    private fun fetchProfiles() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("profiles")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val profileName = documents.first().getString("username") ?: "Profile"
                    binding.button5.text = profileName
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting profiles", e)
            }
    }

    companion object {
        private const val CREATE_PROFILE_REQUEST_CODE = 1
    }
}
