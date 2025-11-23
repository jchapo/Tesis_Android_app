package com.example.android_app.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.android_app.orders.OrderManagementActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            // Usuario ya logueado, verificar custom claims
            debugShowUserClaims()
            // Después de verificar, redirigir a la pantalla principal
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
                    
                    // ⭐ MOSTRAR CUSTOM CLAIMS DESPUÉS DE LOGIN
                    debugShowUserClaims()
                    
                    navigateToOrders()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * FUNCIÓN DEBUG: Muestra en Logcat los custom claims del usuario
     * Útil para verificar que el rol se asignó correctamente
     */
    private fun debugShowUserClaims() {
        CoroutineScope(Dispatchers.Main).launch {
            val user = auth.currentUser
            
            if (user == null) {
                Log.e("CustomClaims", "❌ No hay usuario autenticado")
                return@launch
            }
            
            try {
                // Obtener el token ID y sus claims
                val tokenResult = user.getIdToken(false).await()
                
                Log.d("CustomClaims", "═══════════════════════════════════════════")
                Log.d("CustomClaims", "📋 INFORMACIÓN DEL USUARIO")
                Log.d("CustomClaims", "═══════════════════════════════════════════")
                Log.d("CustomClaims", "UID: ${user.uid}")
                Log.d("CustomClaims", "Email: ${user.email}")
                Log.d("CustomClaims", "Display Name: ${user.displayName}")
                Log.d("CustomClaims", "═══════════════════════════════════════════")
                Log.d("CustomClaims", "🔑 CUSTOM CLAIMS:")
                
                val claims = tokenResult.claims

                // Mostrar TODOS los claims (incluye los estándar de Firebase)
                Log.d("CustomClaims", "Todos los claims:")
                claims.forEach { (key, value) ->
                    Log.i("CustomClaims", "  $key: $value")
                }

                // Verificación específica del rol
                val rol = claims["rol"] as? String
                Log.d("CustomClaims", "═══════════════════════════════════════════")
                
                if (rol != null) {
                    Log.i("CustomClaims", "✅ ROL ENCONTRADO: $rol")
                    
                    when (rol.lowercase()) {
                        "motorizado" -> {
                            Log.i("CustomClaims", "✅ Usuario es MOTORIZADO - Puede hacer uploads ✓")
                        }
                        "admin", "administrador" -> {
                            Log.i("CustomClaims", "✅ Usuario es ADMIN - Acceso completo ✓")
                        }
                        "cliente" -> {
                            Log.i("CustomClaims", "✅ Usuario es CLIENTE - Puede crear pedidos ✓")
                        }
                        else -> {
                            Log.w("CustomClaims", "⚠️ Rol desconocido: $rol")
                        }
                    }
                } else {
                    Log.e("CustomClaims", "❌ NO HAY ROL EN CUSTOM CLAIMS")
                    Log.e("CustomClaims", "⚠️ El usuario NO podrá hacer uploads a Firebase Storage")
                    Log.e("CustomClaims", "💡 Solución:")
                    Log.e("CustomClaims", "   1. Ve a Firebase Console → Authentication")
                    Log.e("CustomClaims", "   2. Busca el usuario: ${user.email}")
                    Log.e("CustomClaims", "   3. Edita Custom claims")
                    Log.e("CustomClaims", "   4. Añade: {\"rol\": \"motorizado\"}")
                }
                
                Log.d("CustomClaims", "═══════════════════════════════════════════")

                // Mostrar Toast visible para el usuario
                if (rol != null) {
                    Toast.makeText(this@LoginActivity, "✅ Rol asignado: $rol", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LoginActivity, "⚠️ Sin rol asignado - Revisar Logcat", Toast.LENGTH_LONG).show()
                }

            } catch (error: Exception) {
                Log.e("CustomClaims", "❌ Error al obtener claims: ${error.message}")
                Log.e("CustomClaims", "Causa: ${error.cause}")
                Toast.makeText(this@LoginActivity, "❌ Error al verificar rol", Toast.LENGTH_SHORT).show()
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