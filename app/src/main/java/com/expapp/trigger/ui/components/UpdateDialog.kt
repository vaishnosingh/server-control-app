package com.expapp.trigger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expapp.trigger.update.DownloadState
import com.expapp.trigger.update.ReleaseInfo
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderDefault
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary
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
    Dialog(onDismissRequest = {
        if (downloadState !is DownloadState.Downloading) {
            onDismiss()
        }
    }) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0C1420), Color(0xFF070B12))
                    )
                )
                .border(1.dp, GreenVibrant.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(GreenVibrant.copy(alpha = 0.22f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, GreenVibrant.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = GreenVibrant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Update Available",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Badge row: v1.0 -> v1.1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "v$currentVersion",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Text("→", fontSize = 14.sp, color = GreenVibrant, fontWeight = FontWeight.Bold)
                    Text(
                        text = releaseInfo.tagName,
                        fontSize = 12.sp,
                        color = GreenVibrant,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenVibrant.copy(alpha = 0.12f))
                            .border(1.dp, GreenVibrant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    if (releaseInfo.sizeBytes > 0) {
                        val mb = String.format("%.1f MB", releaseInfo.sizeBytes / (1024.0 * 1024.0))
                        Text(
                            text = mb,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog Card
                Text(
                    text = "WHAT'S NEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgElevated)
                        .border(1.dp, BorderDefault, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val changelogText = if (releaseInfo.changelog.isNotBlank()) {
                        releaseInfo.changelog.trim()
                    } else {
                        "• Performance improvements & new features."
                    }
                    Text(
                        text = changelogText,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download Progress / States
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (downloadState.bytesDownloaded == 0L) "Connecting to server…" else "Downloading update…",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${downloadState.progressPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenVibrant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (downloadState.bytesDownloaded == 0L && downloadState.progressPercent == 0) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = GreenVibrant,
                                    trackColor = GreenVibrant.copy(alpha = 0.15f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = GreenVibrant,
                                    trackColor = GreenVibrant.copy(alpha = 0.15f)
                                )
                            }
                            if (downloadState.totalBytes > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val downloadedMb = String.format("%.1f", downloadState.bytesDownloaded / (1024.0 * 1024.0))
                                val totalMb = String.format("%.1f MB", downloadState.totalBytes / (1024.0 * 1024.0))
                                Text(
                                    text = "$downloadedMb / $totalMb",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            } else if (downloadState.bytesDownloaded > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val downloadedMb = String.format("%.1f MB", downloadState.bytesDownloaded / (1024.0 * 1024.0))
                                Text(
                                    text = "$downloadedMb downloaded",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    is DownloadState.Failed -> {
                        Text(
                            text = downloadState.error,
                            fontSize = 12.sp,
                            color = RedVibrant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = downloadState !is DownloadState.Downloading,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgElevated)
                            .border(1.dp, BorderDefault, RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            "Later",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenVibrant.copy(alpha = 0.2f),
                            contentColor = GreenVibrant
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (downloadState) {
                                is DownloadState.ReadyToInstall -> Icons.Default.SystemUpdate
                                else -> Icons.Default.Download
                            }
                            val text = when (downloadState) {
                                is DownloadState.ReadyToInstall -> "Install Now"
                                is DownloadState.Failed -> "Retry"
                                is DownloadState.Downloading -> "Downloading…"
                                else -> "Update Now"
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
