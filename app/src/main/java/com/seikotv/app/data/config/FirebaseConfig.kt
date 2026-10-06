package com.seikotv.app.data.config

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FirebaseConfig {

    private const val TAG = "FirebaseConfig"
    private val initMutex = Mutex()

    @Volatile
    private var isInitialized = false

    private var firestoreInstance: FirebaseFirestore? = null
    private var authInstance: FirebaseAuth? = null

    /**
     * Inicializa Firebase Auth y Firestore asegurando que la instancia apunte
     * a la base de datos correcta con persistencia offline activa.
     */
    suspend fun initialize(context: Context): Boolean = initMutex.withLock {
        if (isInitialized && firestoreInstance != null && authInstance != null) {
            return true
        }

        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d(TAG, "Inicializando FirebaseApp desde google-services.json...")
                FirebaseApp.initializeApp(context.applicationContext)
                    ?: throw IllegalStateException("FirebaseApp.initializeApp retornó null")
            } else {
                FirebaseApp.getInstance()
            }

            val projectId = app.options.projectId
            Log.d(TAG, "FirebaseApp activo con projectId: $projectId")

            // Configuración optimizada de Firestore
            val firestore = try {
                FirebaseFirestore.getInstance(app)
            } catch (e: Exception) {
                Log.w(TAG, "Error obteniendo Firestore por app, intentando default: ${e.message}")
                FirebaseFirestore.getInstance()
            }

            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings

            val auth = FirebaseAuth.getInstance(app)

            firestoreInstance = firestore
            authInstance = auth
            isInitialized = true

            Log.i(TAG, "FirebaseConfig inicializado exitosamente (Auth y Firestore vinculados a $projectId)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallo crítico en la inicialización de FirebaseConfig: ${e.message}", e)
            false
        }
    }

    fun isReady(): Boolean = isInitialized && firestoreInstance != null

    fun getFirestore(): FirebaseFirestore {
        return firestoreInstance ?: run {
            Log.w(TAG, "getFirestore() invocado antes de initialize(), usando fallback directo.")
            FirebaseFirestore.getInstance()
        }
    }

    fun getAuth(): FirebaseAuth {
        return authInstance ?: run {
            Log.w(TAG, "getAuth() invocado antes de initialize(), usando fallback directo.")
            FirebaseAuth.getInstance()
        }
    }

    fun getProjectId(): String? {
        return try {
            FirebaseApp.getInstance().options.projectId
        } catch (_: Exception) {
            null
        }
    }
}
