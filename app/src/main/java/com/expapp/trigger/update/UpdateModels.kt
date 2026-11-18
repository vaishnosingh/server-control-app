package com.expapp.trigger.update

import java.io.File

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val apkFileName: String,
    val sizeBytes: Long
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: ReleaseInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class ReadyToInstall(val apkFile: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
