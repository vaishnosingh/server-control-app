package com.expapp.trigger.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdateManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    val githubRepo: String = "vaishnosingh/server-control-app"
) {
    private val mainHandler = try {
        val looper = Looper.getMainLooper()
        if (looper != null) Handler(looper) else null
    } catch (e: Throwable) {
        null
    }

    private fun runOnMain(block: () -> Unit) {
        mainHandler?.post(block) ?: block()
    }

    /**
     * Checks GitHub Releases API for the latest published release.
     */
    fun checkForUpdate(
        currentVersionName: String,
        callback: (UpdateCheckResult) -> Unit
    ) {
        // Query releases list so we always inspect the highest semantic version
        val url = "https://api.github.com/repos/$githubRepo/releases"

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "ServerControlApp")
            .get()
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnMain {
                    callback(UpdateCheckResult.Error("Network error: ${e.localizedMessage}"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (res.code == 404) {
                        runOnMain { callback(UpdateCheckResult.UpToDate) }
                        return
                    }
                    if (!res.isSuccessful) {
                        runOnMain {
                            callback(UpdateCheckResult.Error("HTTP ${res.code}: Unable to check updates"))
                        }
                        return
                    }

                    val body = res.body?.string().orEmpty()
                    try {
                        val jsonElement = JsonParser.parseString(body)
                        val releasesArray = if (jsonElement.isJsonArray) {
                            jsonElement.asJsonArray
                        } else {
                            // In case a single release object was returned
                            val arr = com.google.gson.JsonArray()
                            if (jsonElement.isJsonObject) arr.add(jsonElement.asJsonObject)
                            arr
                        }

                        var highestRelease: ReleaseInfo? = null

                        for (item in releasesArray) {
                            if (!item.isJsonObject) continue
                            val json = item.asJsonObject
                            if (json.has("draft") && json.get("draft").asBoolean) continue

                            val tagName = json.get("tag_name")?.asString.orEmpty()
                            val cleanVer = tagName.removePrefix("v").removePrefix("V").trim()
                            val title = json.get("name")?.asString?.ifBlank { tagName } ?: tagName
                            val changelog = json.get("body")?.asString.orEmpty()

                            val assets = json.getAsJsonArray("assets")
                            var apkUrl: String? = null
                            var apkName = "app-release.apk"
                            var apkSize = 0L

                            if (assets != null) {
                                for (element in assets) {
                                    val assetObj = element.asJsonObject
                                    val name = assetObj.get("name")?.asString.orEmpty()
                                    if (name.endsWith(".apk", ignoreCase = true)) {
                                        apkUrl = assetObj.get("browser_download_url")?.asString
                                        apkName = name
                                        apkSize = assetObj.get("size")?.asLong ?: 0L
                                        break
                                    }
                                }
                            }

                            if (!apkUrl.isNullOrBlank()) {
                                val releaseInfo = ReleaseInfo(
                                    tagName = tagName,
                                    versionName = cleanVer,
                                    title = title,
                                    changelog = changelog,
                                    downloadUrl = apkUrl,
                                    apkFileName = apkName,
                                    sizeBytes = apkSize
                                )

                                if (highestRelease == null || isNewerVersion(cleanVer, highestRelease.versionName)) {
                                    highestRelease = releaseInfo
                                }
                            }
                        }

                        if (highestRelease == null) {
                            runOnMain { callback(UpdateCheckResult.UpToDate) }
                            return
                        }

                        val cleanLocalVersion = currentVersionName.removePrefix("v").removePrefix("V").trim()

                        if (isNewerVersion(highestRelease.versionName, cleanLocalVersion)) {
                            runOnMain {
                                callback(UpdateCheckResult.UpdateAvailable(highestRelease))
                            }
                        } else {
                            runOnMain { callback(UpdateCheckResult.UpToDate) }
                        }
                    } catch (e: Exception) {
                        runOnMain {
                            callback(UpdateCheckResult.Error("Failed to parse update info: ${e.message}"))
                        }
                    }
                }
            }
        })
    }

    /**
     * Downloads the APK file to the app's cache directory and streams progress updates.
     */
    fun downloadApk(
        downloadUrl: String,
        onProgress: (DownloadState) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(downloadUrl)
            .addHeader("User-Agent", "ServerControlApp")
            .get()
            .build()

        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    runOnMain {
                        onProgress(DownloadState.Failed("Download failed: ${e.localizedMessage}"))
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnMain {
                        onProgress(DownloadState.Failed("Server returned HTTP ${response.code}"))
                    }
                    return
                }

                val responseBody = response.body
                if (responseBody == null) {
                    runOnMain {
                        onProgress(DownloadState.Failed("Response body was empty"))
                    }
                    return
                }

                val updateDir = File(context.cacheDir, "updates")
                if (!updateDir.exists()) {
                    updateDir.mkdirs()
                }
                val apkFile = File(updateDir, "update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                try {
                    val totalBytes = responseBody.contentLength()
                    var bytesDownloaded = 0L

                    responseBody.byteStream().use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var lastPercent = -1

                            while (input.read(buffer).also { read = it } != -1) {
                                if (call.isCanceled()) return

                                output.write(buffer, 0, read)
                                bytesDownloaded += read

                                val percent = if (totalBytes > 0) {
                                    ((bytesDownloaded * 100) / totalBytes).toInt()
                                } else 0

                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    runOnMain {
                                        onProgress(
                                            DownloadState.Downloading(
                                                progressPercent = percent,
                                                bytesDownloaded = bytesDownloaded,
                                                totalBytes = totalBytes
                                            )
                                        )
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    runOnMain {
                        onProgress(DownloadState.ReadyToInstall(apkFile))
                    }
                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        runOnMain {
                            onProgress(DownloadState.Failed("File write failed: ${e.localizedMessage}"))
                        }
                    }
                }
            }
        })
        return call
    }

    /**
     * Prompts Android package installer to install the downloaded APK.
     */
    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        // On Android 8.0+, check if installing from unknown sources is permitted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            apkFile.inputStream().use { input ->
                session.openWrite("package", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val receiverIntent = Intent(context, InstallStatusReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                receiverIntent,
                flags
            )

            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (e: Exception) {
            // Fallback to FileProvider VIEW intent if session API is unsupported by vendor ROM
            fallbackInstall(apkFile)
        }
    }

    private fun fallbackInstall(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (_: Exception) {}
    }

    companion object {
        /**
         * Semantic version comparator. Returns true if remote is strictly greater than local.
         */
        fun isNewerVersion(remote: String, local: String): Boolean {
            val remoteParts = remote.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
            val localParts = local.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }
    }
}
