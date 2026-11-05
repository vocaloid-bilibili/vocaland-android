package xyz.lisfox.vocaland.ui.share

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.share.CollectionShareCardData
import xyz.lisfox.vocaland.data.share.SHARE_CARD_HEIGHT_DP
import xyz.lisfox.vocaland.data.share.SHARE_CARD_HEIGHT_PX
import xyz.lisfox.vocaland.data.share.SHARE_CARD_WIDTH_DP
import xyz.lisfox.vocaland.data.share.SHARE_CARD_WIDTH_PX
import xyz.lisfox.vocaland.data.share.ShareFileStore
import xyz.lisfox.vocaland.data.remote.toUserMessage

/**
 * 分享卡预览 + PNG 导出。
 *
 * 卡片本身按 540×960 dp 排版，预览时按可用宽度等比缩放（只影响显示）；
 * 导出时从 GraphicsLayer 捕获未缩放内容，再缩放到 1080×1920 写入相册。
 * 图片是异步加载的，导出前会稍等一小段时间，尽量减少封面占位。
 */
@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun ShareCardPreview(
    data: CollectionShareCardData,
    modifier: Modifier = Modifier,
    onSaved: (Uri) -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var exporting by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(
                SHARE_CARD_WIDTH_DP.toFloat() / SHARE_CARD_HEIGHT_DP.toFloat(),
            )
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val scale = maxWidth.value / SHARE_CARD_WIDTH_DP
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .align(Alignment.TopStart),
            ) {
                CollectionShareCard(
                    data = data,
                    modifier = Modifier.drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    },
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                if (exporting) return@OutlinedButton
                exporting = true
                scope.launch {
                    try {
                        delay(200)
                        val captured = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        val scaled = Bitmap.createScaledBitmap(
                            captured,
                            SHARE_CARD_WIDTH_PX,
                            SHARE_CARD_HEIGHT_PX,
                            true,
                        )
                        ShareFileStore.saveImage(context, scaled)
                            .onSuccess { uri ->
                                onSaved(uri)
                            }
                            .onFailure { error ->
                                onError(error.toUserMessage())
                            }
                    } catch (error: Throwable) {
                        onError(error.toUserMessage())
                    } finally {
                        exporting = false
                    }
                }
            },
            enabled = !exporting,
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(if (exporting) " 导出中…" else " 生成分享图")
        }
    }
}

/** 分享入口按钮：外部页面（如收藏册）用它触发分享流程。 */
@Composable
fun ShareCardExportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(" 生成分享图")
    }
}
