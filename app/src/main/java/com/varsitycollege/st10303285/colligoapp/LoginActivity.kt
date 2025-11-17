package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.util.concurrent.Executor
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var btnGoogleSignIn: Button

    private lateinit var googleClient: GoogleSignInClient
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val acct = task.getResult(ApiException::class.java)
            val idToken = acct?.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                Toast.makeText(this, "Google sign-in failed (no token)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in error: ${e.statusCode}", Toast.LENGTH_LONG).show()
            Log.w("LoginActivity", "Google sign in failed", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.passwordEditText)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.registerRedirect) // TextView in layout
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        btnLogin.setOnClickListener { doLogin() }
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Google Sign-in setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
        btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleClient.signInIntent)
        }

        // If user already logged in, try biometric quick unlock
        if (auth.currentUser != null) {
            tryShowBiometricPrompt()
        }
    }

    private fun doLogin() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString()

        if (email.isEmpty() || pass.length < 6) {
            Toast.makeText(this, "Enter a valid email and password (min 6 chars)", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Firebase auth with Google failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // ---- Biometric helpers using androidx.biometric ----

    private fun tryShowBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showBiometricPrompt()
            else -> {
                // device doesn't support biometrics or none enrolled
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(this)

        // Build the prompt info using the androidx.biometric class
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Colligo")
            .setSubtitle("Use your fingerprint or device credential")
            .setNegativeButtonText("Use password")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // biometric success -> go to main
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // optional: show toast or log
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }
}