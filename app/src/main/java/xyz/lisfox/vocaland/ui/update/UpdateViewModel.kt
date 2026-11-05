package xyz.lisfox.vocaland.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.update.AppUpdate
import xyz.lisfox.vocaland.data.update.UpdateDownloadSource
import xyz.lisfox.vocaland.data.update.UpdateDownloader
import xyz.lisfox.vocaland.data.update.UpdateRepository

enum class UpdateDownloadStatus {
    Downloading,
    Completed,
    Failed,
}

data class UpdateDownloadState(
    val versionName: String,
    val source: UpdateDownloadSource,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val status: UpdateDownloadStatus = UpdateDownloadStatus.Downloading,
    val apkFile: File? = null,
    val error: String? = null,
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class UpdateUiState(
    val checking: Boolean = false,
    val update: AppUpdate? = null,
    val visible: Boolean = false,
    val message: String? = null,
    val download: UpdateDownloadState? = null,
)

class UpdateViewModel(
    private val repository: UpdateRepository,
    private val downloader: UpdateDownloader,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var notifyOnComplete = false
    private var downloadJob: Job? = null

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean) {
        if (_state.value.checking) {
            if (!silent) {
                notifyOnComplete = true
                _state.update { it.copy(message = "正在检查更新…") }
            }
            return
        }

        notifyOnComplete = !silent
        _state.update {
            it.copy(
                checking = true,
                message = if (silent) it.message else "正在检查更新…",
            )
        }
        viewModelScope.launch {
            try {
                val update = repository.checkForUpdate()
                val notify = notifyOnComplete
                notifyOnComplete = false
                _state.update {
                    it.copy(
                        checking = false,
                        update = update,
                        visible = update != null,
                        message = if (update == null && notify) "当前已是最新版本" else null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val notify = notifyOnComplete
                notifyOnComplete = false
                _state.update {
                    it.copy(
                        checking = false,
                        message = if (notify) updateErrorMessage(error) else null,
                    )
                }
            }
        }
    }

    fun startDownload(update: AppUpdate, source: UpdateDownloadSource) {
        if (_state.value.download?.status == UpdateDownloadStatus.Downloading) return
        downloadJob?.cancel()
        _state.update {
            it.copy(
                visible = false,
                download = UpdateDownloadState(
                    versionName = update.versionName,
                    source = source,
                ),
            )
        }
        downloadJob = viewModelScope.launch {
            val job = coroutineContext[Job]
            try {
                val file = downloader.download(update, source) { downloaded, total ->
                    if (downloadJob === job) {
                        _state.update { current ->
                            val download = current.download ?: return@update current
                            current.copy(
                                download = download.copy(
                                    downloadedBytes = downloaded,
                                    totalBytes = if (total > 0) total else download.totalBytes,
                                ),
                            )
                        }
                    }
                }
                if (downloadJob !== job) return@launch
                _state.update { current ->
                    val download = current.download ?: return@update current
                    current.copy(
                        download = download.copy(
                            status = UpdateDownloadStatus.Completed,
                            apkFile = file,
                            downloadedBytes = file.length(),
                            totalBytes = file.length().takeIf { it > 0 } ?: download.totalBytes,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                if (downloadJob === job) {
                    _state.update { it.copy(download = null) }
                }
                throw error
            } catch (error: Exception) {
                if (downloadJob !== job) return@launch
                _state.update { current ->
                    val download = current.download ?: return@update current
                    current.copy(
                        download = download.copy(
                            status = UpdateDownloadStatus.Failed,
                            error = downloadErrorMessage(error),
                        ),
                    )
                }
            }
        }
    }

    fun closeDownload() {
        if (_state.value.download?.status == UpdateDownloadStatus.Downloading) {
            cancelDownload()
        } else {
            _state.update { it.copy(download = null) }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.update { it.copy(download = null) }
    }

    fun dismiss() {
        _state.update { it.copy(visible = false) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun updateErrorMessage(error: Exception): String =
        if (error is IOException) "网络连接失败，请检查网络后重试" else "检查更新失败，请稍后重试"

    private fun downloadErrorMessage(error: Exception): String =
        if (error is IOException) "下载失败，请检查网络后重试" else "下载失败，请稍后重试"
}
