package com.example.android_app.config

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Clase Application para inicializar Firebase
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Configurar Firestore
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Habilitar caché offline
            .build()
        firestore.firestoreSettings = settings

        println("✅ Firebase inicializado correctamente")
    }
}