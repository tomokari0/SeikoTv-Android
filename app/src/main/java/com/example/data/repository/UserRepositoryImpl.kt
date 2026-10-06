package com.example.data.repository

import android.util.Log
import com.example.data.datasource.local.UserPreferencesDataStore
import com.example.domain.model.UserProfile
import com.example.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.seikotv.app.data.config.FirebaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseConfig.getFirestore()
        } catch (_: Exception) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Exception) {
                null
            }
        }
    }
) : UserRepository {

    companion object {
        private const val TAG = "UserRepository"
        private const val COLLECTION_PROFILES = "profiles"

        val DEFAULT_PROFILES = listOf(
            UserProfile(id = "p1", name = "Seiko", avatarColorHex = 0xFFE50914, isKids = false),
            UserProfile(id = "p2", name = "Alex", avatarColorHex = 0xFF3B82F6, isKids = false),
            UserProfile(id = "p3", name = "Kira", avatarColorHex = 0xFF8B5CF6, isKids = false),
            UserProfile(id = "p4", name = "Kids", avatarColorHex = 0xFF10B981, isKids = true)
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _profilesFlow = MutableStateFlow<List<UserProfile>>(DEFAULT_PROFILES)
    private val _activeIdFlow = MutableStateFlow<String>("p1")
    private var firestoreListener: ListenerRegistration? = null

    init {
        // Observar activeProfileId guardado en DataStore para mantener sincronizado el ID activo
        scope.launch {
            try {
                preferencesDataStore.activeProfileId.collect { idFromDataStore ->
                    if (idFromDataStore.isNotBlank()) {
                        _activeIdFlow.value = idFromDataStore
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error leyendo activeProfileId de DataStore: ${e.message}")
            }
        }

        // Conectar a Firestore profiles si está disponible
        initFirestoreListener()
    }

    private fun initFirestoreListener() {
        val db = try {
            firestoreProvider()
        } catch (e: Exception) {
            Log.w(TAG, "Error obteniendo Firestore: ${e.message}")
            null
        } ?: return

        Log.d(TAG, "Inicializando snapshot listener en '$COLLECTION_PROFILES' de Firestore...")
        firestoreListener?.remove()
        firestoreListener = db.collection(COLLECTION_PROFILES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error en Firestore '$COLLECTION_PROFILES': ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val name = (data["name"] as? String)
                                ?: (data["displayName"] as? String)
                                ?: doc.id
                            val avatarUrl = (data["avatarUrl"] as? String)
                                ?: (data["photoUrl"] as? String)
                                ?: ""
                            val colorVal = (data["avatarColorHex"] as? Number)?.toLong()
                                ?: (data["colorHex"] as? Number)?.toLong()
                                ?: getColorForName(name)
                            val isKids = (data["isKids"] as? Boolean)
                                ?: (data["kids"] as? Boolean)
                                ?: name.equals("kids", ignoreCase = true)

                            UserProfile(
                                id = doc.id,
                                name = name,
                                avatarUrl = avatarUrl,
                                avatarColorHex = colorVal,
                                isKids = isKids
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando documento de perfil '${doc.id}': ${e.message}")
                            null
                        }
                    }

                    if (list.isNotEmpty()) {
                        _profilesFlow.value = list
                    }
                } else {
                    Log.d(TAG, "Colección '$COLLECTION_PROFILES' vacía en Firestore. Sembrando perfiles por defecto...")
                    scope.launch {
                        seedDefaultProfiles(db)
                    }
                }
            }
    }

    override fun getProfiles(): Flow<List<UserProfile>> = _profilesFlow.asStateFlow()

    override fun getActiveProfile(): Flow<UserProfile> {
        return combine(
            _activeIdFlow,
            _profilesFlow
        ) { activeId, profiles ->
            profiles.firstOrNull { it.id == activeId }
                ?: profiles.firstOrNull { it.name.equals(activeId, ignoreCase = true) }
                ?: profiles.firstOrNull()
                ?: DEFAULT_PROFILES.first()
        }.distinctUntilChanged()
    }

    override suspend fun selectProfile(profile: UserProfile) {
        Log.d(TAG, "Seleccionando perfil: id='${profile.id}', name='${profile.name}'")
        _activeIdFlow.value = profile.id
        try {
            preferencesDataStore.setActiveProfile(profile.id, profile.name)
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando perfil activo en DataStore: ${e.message}")
        }
    }

    override suspend fun createProfile(name: String, colorHex: Long, isKids: Boolean) {
        val profileId = "profile_${System.currentTimeMillis()}"
        val newProfile = UserProfile(
            id = profileId,
            name = name,
            avatarColorHex = colorHex,
            isKids = isKids
        )
        _profilesFlow.value = _profilesFlow.value + newProfile

        val db = try {
            firestoreProvider()
        } catch (_: Exception) {
            null
        }

        if (db != null) {
            try {
                val profileMap = hashMapOf<String, Any>(
                    "id" to profileId,
                    "name" to name,
                    "avatarColorHex" to colorHex,
                    "isKids" to isKids,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_PROFILES).document(profileId).set(profileMap).await()
                Log.d(TAG, "Perfil '$name' creado y sincronizado en Firestore ('$profileId')")
            } catch (e: Exception) {
                Log.w(TAG, "Error sincronizando nuevo perfil en Firestore: ${e.message}")
            }
        }
    }

    private suspend fun seedDefaultProfiles(db: FirebaseFirestore) {
        try {
            DEFAULT_PROFILES.forEach { p ->
                val profileMap = hashMapOf<String, Any>(
                    "id" to p.id,
                    "name" to p.name,
                    "avatarColorHex" to p.avatarColorHex,
                    "isKids" to p.isKids,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_PROFILES).document(p.id).set(profileMap).await()
            }
            Log.d(TAG, "Perfiles por defecto guardados en Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Error sembrando perfiles en Firestore: ${e.message}")
        }
    }

    private fun getColorForName(name: String): Long {
        return when (name.lowercase().trim()) {
            "seiko" -> 0xFFE50914
            "alex" -> 0xFF3B82F6
            "kira" -> 0xFF8B5CF6
            "kids" -> 0xFF10B981
            else -> {
                val colors = listOf(0xFFE50914, 0xFF3B82F6, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899)
                colors[kotlin.math.abs(name.hashCode()) % colors.size]
            }
        }
    }
}
