package com.varsitycollege.st10303285.colligoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.varsitycollege.st10303285.colligoapp.R

// Registration activity for new users
class RegisterActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etFullName: EditText
    private lateinit var etUniversity: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.emailEditText)
        etFullName = findViewById(R.id.fullNameEditText)
        etUniversity = findViewById(R.id.etUniversity)
        etPassword = findViewById(R.id.passwordEditText)
        btnSignUp = findViewById(R.id.btnRegister)

        btnSignUp.setOnClickListener { doSignUp() }
    }
    private fun doSignUp() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString()
        val name = etFullName.text.toString().trim()
        val uni = etUniversity.text.toString().trim()

        if (email.isEmpty() || pass.length < 6 || name.isEmpty() || uni.isEmpty()) {
            Toast.makeText(this, "Please fill all fields, password must be 6+ chars", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save extra profile info to Firestore under collection "users"
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userDoc = hashMapOf(
                        "email" to email,
                        "fullName" to name,
                        "university" to uni,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(uid).set(userDoc)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

}