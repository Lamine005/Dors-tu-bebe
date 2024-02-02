package com.example.dorstu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.example.dorstu.databinding.ActivityPageConnexionBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import com.google.android.gms.auth.api.signin.GoogleSignIn

class PageConnexion : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPageConnexionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
        super.onCreate(savedInstanceState)

        binding = ActivityPageConnexionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = Firebase.auth

        binding.btnConnexion.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if(checkAllField()) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if(it.isSuccessful){
                        Toast.makeText(this,"connecté avec succès", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, ChoisirProfil::class.java)
                        startActivity(intent)

                        finish()
                    } else {
                        Log.e("error: ", it.exception.toString())
                    }
                }

            }
        }

        val button = findViewById<Button>(R.id.btn_inscription)
        button.setOnClickListener {
            val intent = Intent(this, PageCreer::class.java)
            startActivity(intent)
        }
    }
    private fun checkAllField(): Boolean {
        val email = binding.email.text.toString()
        if (binding.email.text.toString() == "") {
            binding.textInputLayoutEmail.error = "Ce champ est obligatoire"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.error = "vérifier le format du courriel"
            return false
        }

        if (binding.password.text.toString() == "") {
            binding.textInputLayoutPassword.error = "Ce champ est obligatoire"
            binding.textInputLayoutPassword.errorIconDrawable = null
            return false
        }

        if (binding.password.length() < 5) {
            binding.textInputLayoutPassword.error =
                "Le mot de passe doit contenir au moins 6 caractères"
            binding.textInputLayoutEmail.errorIconDrawable = null
            return false
        }
        return true
    }
}


