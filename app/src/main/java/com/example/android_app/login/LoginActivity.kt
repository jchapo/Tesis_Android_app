package com.example.android_app.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.android_app.orders.OrderManagementActivity
import com.google.firebase.auth.FirebaseAuth
import kotlin.jvm.java

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // Inicializar FirebaseAuth

        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginClick = { email, password ->
                        signInWithFirebase(email, password)
                    },
                    onForgotPasswordClick = { email ->
                        sendPasswordReset(email)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya logueado, redirigir a la pantalla principal
            navigateToOrders()
        }
    }

    private fun signInWithFirebase(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Por favor, ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Bienvenido ${user?.email}", Toast.LENGTH_SHORT).show()
                    navigateToOrders()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            Toast.makeText(this, "Ingresa tu correo electrónico primero", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Correo de recuperación enviado a $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToOrders() {
        Toast.makeText(this, "Redirigiendo a la pantalla principal...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, OrderManagementActivity::class.java)
        startActivity(intent)
        finish()
    }

}