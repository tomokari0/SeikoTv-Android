package com.example.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = downloadRepository.getDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(id)
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(id)
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id)
        }
    }
}
