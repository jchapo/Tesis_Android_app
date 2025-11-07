package com.example.android_app.Orders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.android_app.Login.LoginScreen

class OrderManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginClick = { user, pass ->
                        // Lógica de login
                    },
                    onForgotPasswordClick = {
                        // Recuperar contraseña
                    }
                )
            }
        }
    }
}
