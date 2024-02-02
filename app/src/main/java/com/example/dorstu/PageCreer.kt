package com.example.dorstu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.dorstu.databinding.ActivityPageCreerBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth


class PageCreer : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPageCreerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPageCreerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.createAccountButton.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (checkAllField()) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.signOut()
                        Toast.makeText(this, "Compte créé avec succès!", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            binding.textInputLayoutEmail.error = "Cette adresse email est déjà utilisée par un autre compte."
                        } else {
                            Log.e("error: ", task.exception.toString())
                            Toast.makeText(this, "Échec de la création du compte: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
        private fun checkAllField(): Boolean {
            val email = binding.etEmail.text.toString()
            if (binding.etEmail.text.toString() == "") {
                binding.textInputLayoutEmail.error = "Ce champ est obligatoire"
                return false
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textInputLayoutEmail.error = "vérifier le format du courriel"
                return false
            }

            if (binding.etPassword.text.toString() == "") {
                binding.textInputLayoutPassword.error = "Ce champ est obligatoire"
                binding.textInputLayoutPassword.errorIconDrawable = null
                return false
            }

            if (binding.etPassword.length() < 5) {
                binding.textInputLayoutPassword.error =
                    "Le mot de passe doit contenir au moins 6 caractères"
                binding.textInputLayoutEmail.errorIconDrawable = null
                return false
            }

            if (binding.etConfirmPassword.text.toString() == "") {
                binding.textInputLayoutConfirmPassword.error = "Ce champ est obligatoire"
                binding.textInputLayoutConfirmPassword.errorIconDrawable = null
                return false
            }

            if (binding.etPassword.text.toString() != binding.etConfirmPassword.text.toString()) {
                binding.textInputLayoutPassword.error = "Le mot de passe ne correspond pas"
                return false
            }
            return true
        }
        private fun navigateToLogin() {
            val intent = Intent(this, PageConnexion::class.java)
            startActivity(intent)
            finish()
    }
}
