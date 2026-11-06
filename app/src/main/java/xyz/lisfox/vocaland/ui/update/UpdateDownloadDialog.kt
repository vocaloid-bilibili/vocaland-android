package xyz.lisfox.vocaland.ui.update

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.data.update.UpdateDownloadSource

@Composable
fun UpdateDownloadDialog(
    download: UpdateDownloadState,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onInstall: () -> Unit,
) {
    Dialog(
        onDismissRequest = {
            if (download.status == UpdateDownloadStatus.Downloading) {
                onCancel()
            } else {
                onClose()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = when (download.status) {
                        UpdateDownloadStatus.Downloading -> "正在下载 ${download.versionName}"
                        UpdateDownloadStatus.Completed -> "下载完成"
                        UpdateDownloadStatus.Failed -> "下载失败"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (download.status) {
                        UpdateDownloadStatus.Downloading -> "下载来源：${download.source.label()}"
                        UpdateDownloadStatus.Completed -> "VocaLand ${download.versionName} 已下载"
                        UpdateDownloadStatus.Failed -> download.error ?: "下载失败，请稍后重试"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                when (download.status) {
                    UpdateDownloadStatus.Downloading -> DownloadingContent(
                        download = download,
                        onCancel = onCancel,
                    )
                    UpdateDownloadStatus.Completed -> {
                        DownloadCompletedContent(
                            download = download,
                            onClose = onClose,
                            onInstall = onInstall,
                        )
                    }
                    UpdateDownloadStatus.Failed -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(
                                onClick = onClose,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("关闭")
                            }
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    download: UpdateDownloadState,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    if (download.totalBytes > 0) {
        LinearProgressIndicator(
            progress = { download.progress },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        InlineLoading()
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (download.totalBytes > 0) {
                "${formatBytes(context, download.downloadedBytes)} / " +
                    formatBytes(context, download.totalBytes)
            } else {
                formatBytes(context, download.downloadedBytes)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (download.totalBytes > 0) {
                "${(download.progress * 100).toInt()}%"
            } else {
                "下载中"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(18.dp))
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("取消下载")
    }
}

@Composable
private fun DownloadCompletedContent(
    download: UpdateDownloadState,
    onClose: () -> Unit,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    Text(
        text = "安装包大小：${formatBytes(context, download.apkFile?.length() ?: 0L)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(18.dp))
    Button(
        onClick = onInstall,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("安装")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(onClick = onClose) {
            Text("稍后")
        }
    }
}

private fun UpdateDownloadSource.label(): String = when (this) {
    UpdateDownloadSource.GitHub -> "GitHub"
    UpdateDownloadSource.Mirror -> "镜像加速"
}

private fun formatBytes(context: android.content.Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)
