package com.seikotv.app.data.config

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseConnectionInterceptor(
    private val context: Context
) {

    companion object {
        private const val TAG = "FirebaseInterceptor"
    }

    /**
     * Comprueba si el dispositivo tiene conectividad a internet activa.
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Intercepta la consulta a Firestore verificando previamente:
     * 1. Que FirebaseConfig esté correctamente inicializado.
     * 2. Que la instancia de Firestore esté disponible y apuntando al proyecto correcto.
     * 3. Registra el estado de la conexión (Online vs Offline Cache).
     *
     * @param operationName Nombre descriptivo de la operación (ej: "FetchContentCollection")
     * @param queryBlock Lambda que ejecuta la consulta recibiendo la instancia validada de Firestore
     */
    suspend fun <T> intercept(
        operationName: String,
        queryBlock: suspend (FirebaseFirestore) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[$operationName] Interceptando consulta a Firestore...")

        // 1. Verificación de inicialización previa
        if (!FirebaseConfig.isReady()) {
            Log.w(TAG, "[$operationName] FirebaseConfig no estaba listo. Intentando inicialización dinámica...")
            val success = FirebaseConfig.initialize(context)
            if (!success) {
                val errorMsg = "[$operationName] RECHAZADO: Firebase no se pudo inicializar antes de la consulta."
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }
        }

        // 2. Comprobación del estado de red
        val isOnline = isNetworkAvailable()
        if (isOnline) {
            Log.d(TAG, "[$operationName] Estado de red: ONLINE. Proyecto: ${FirebaseConfig.getProjectId()}")
        } else {
            Log.w(TAG, "[$operationName] Estado de red: OFFLINE. La consulta utilizará la caché local de Firestore.")
        }

        // 3. Ejecución segura de la consulta
        try {
            val firestore = FirebaseConfig.getFirestore()
            val result = queryBlock(firestore)
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "[$operationName] Consulta completada exitosamente en ${elapsed}ms")
            Result.success(result)
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "[$operationName] Error al ejecutar consulta tras ${elapsed}ms: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica rápidamente si Firestore está listo para recibir operaciones.
     */
    suspend fun ensureReady(): Boolean {
        return if (FirebaseConfig.isReady()) {
            true
        } else {
            FirebaseConfig.initialize(context)
        }
    }
}
