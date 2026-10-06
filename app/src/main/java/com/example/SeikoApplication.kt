package com.example

import android.app.Application
import android.util.Log
import com.example.data.datasource.local.SeikoDatabase
import com.example.data.datasource.local.UserPreferencesDataStore
import com.example.data.datasource.remote.FirestoreContentDataSource
import com.example.data.repository.ContentRepositoryImpl
import com.example.data.repository.DownloadRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.repository.ContentRepository
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.seikotv.app.data.config.FirebaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SeikoApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: SeikoDatabase by lazy { SeikoDatabase.getInstance(this) }
    val preferences: UserPreferencesDataStore by lazy { UserPreferencesDataStore(this) }
    val firestoreDataSource: FirestoreContentDataSource by lazy { FirestoreContentDataSource() }

    val contentRepository: ContentRepository by lazy {
        ContentRepositoryImpl(
            firestoreDataSource = firestoreDataSource,
            watchProgressDao = database.watchProgressDao(),
            myListDao = database.myListDao()
        )
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(
            context = this,
            downloadDao = database.downloadDao(),
            applicationScope = applicationScope
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(preferences)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initFirebase()
    }

    private fun initFirebase() {
        applicationScope.launch {
            val initialized = FirebaseConfig.initialize(this@SeikoApplication)
            if (initialized) {
                Log.d("SeikoApp", "FirebaseConfig inicializado exitosamente en SeikoApplication.")
            } else {
                Log.w("SeikoApp", "Fallo al inicializar FirebaseConfig.")
            }
        }
    }

    companion object {
        lateinit var instance: SeikoApplication
            private set
    }
}
