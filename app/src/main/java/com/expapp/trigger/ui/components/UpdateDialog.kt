package com.expapp.trigger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expapp.trigger.ui.theme.LocalIsDarkTheme
import com.expapp.trigger.ui.theme.MonoFontFamily
import com.expapp.trigger.ui.theme.PixelSemantic
import com.expapp.trigger.update.DownloadState
import com.expapp.trigger.update.ReleaseInfo
import java.io.File

@Composable
fun UpdateDialog(
    releaseInfo: ReleaseInfo,
    currentVersion: String,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    val sizeText = if (releaseInfo.sizeBytes > 0L) {
        String.format("%.1f MB", releaseInfo.sizeBytes / (1024.0 * 1024.0))
    } else null

    Dialog(
        onDismissRequest = {
            if (downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PixelSemantic.onlineContainer(dark)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = PixelSemantic.onOnlineContainer(dark),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Update Available",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (releaseInfo.title.isNotBlank()) {
                            Text(
                                text = releaseInfo.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Version comparison pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "v$currentVersion → v${releaseInfo.versionName}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (sizeText != null) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = PixelSemantic.onlineContainer(dark)
                        ) {
                            Text(
                                text = sizeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = PixelSemantic.onOnlineContainer(dark),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (releaseInfo.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = releaseInfo.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Spacer(modifier = Modifier.height(14.dp))
                        val hasKnownTotal = downloadState.totalBytes > 0L
                        if (hasKnownTotal) {
                            LinearProgressIndicator(
                                progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val dlMb = String.format("%.1f", downloadState.bytesDownloaded / (1024.0 * 1024.0))
                            val totalMb = String.format("%.1f", downloadState.totalBytes / (1024.0 * 1024.0))
                            Text(
                                text = "$dlMb / $totalMb MB (${downloadState.progressPercent}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            if (downloadState.bytesDownloaded > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val dlMb = String.format("%.1f MB", downloadState.bytesDownloaded / (1024.0 * 1024.0))
                                Text(
                                    text = "$dlMb downloaded",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is DownloadState.Failed -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = downloadState.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = downloadState !is DownloadState.Downloading,
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Later")
                    }

                    Button(
                        onClick = {
                            when (downloadState) {
                                is DownloadState.ReadyToInstall -> onInstall(downloadState.apkFile)
                                is DownloadState.Downloading -> {}
                                else -> onDownload()
                            }
                        },
                        enabled = downloadState !is DownloadState.Downloading,
                        shape = RoundedCornerShape(100),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PixelSemantic.onlineContainer(dark),
                            contentColor = PixelSemantic.onOnlineContainer(dark)
                        ),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        val icon = when (downloadState) {
                            is DownloadState.ReadyToInstall -> Icons.Default.SystemUpdate
                            else -> Icons.Default.Download
                        }
                        val label = when (downloadState) {
                            is DownloadState.ReadyToInstall -> "Install Now"
                            is DownloadState.Failed -> "Retry"
                            is DownloadState.Downloading -> "Downloading…"
                            else -> "Update Now"
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
