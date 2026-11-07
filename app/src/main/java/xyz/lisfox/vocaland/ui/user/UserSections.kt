package xyz.lisfox.vocaland.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.LevelInfoDto
import xyz.lisfox.vocaland.data.model.PublicProfileResponseDto
import xyz.lisfox.vocaland.data.model.WalletDto
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.formatNumber

@Composable
internal fun UserProfileCard(
    profile: PublicProfileResponseDto?,
    isLoggedIn: Boolean,
    nickname: String,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = "简介")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    !isLoggedIn -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "登录后可查看 $nickname 的简介与链接。",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = onLogin) {
                                Text("去登录")
                            }
                        }
                    }

                    profile == null -> Text(
                        text = "$nickname 的主页资料暂时无法加载。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> {
                        val data = profile.profile
                        val hasAny = !data.bio.isNullOrBlank() ||
                            !data.content.isNullOrBlank() ||
                            data.links.isNotEmpty()

                        if (!hasAny) {
                            Text(
                                text = "$nickname 还没有填写个人简介。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            data.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            data.content?.takeIf { it.isNotBlank() }?.let { content ->
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            data.links.forEach { link ->
                                Text(
                                    text = "${link.label ?: link.platform} · ${link.url}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UserOwnerSection(
    wallet: WalletDto?,
    level: LevelInfoDto?,
    loading: Boolean,
    error: String?,
    notice: String?,
    checkinBusy: Boolean,
    onCheckin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallet: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val checkedIn = level?.checkedInToday == true
    val progress = level?.let { info ->
        if (info.nextLevelExp > 0) {
            (info.currentLevelExp.toFloat() / info.nextLevelExp.toFloat()).coerceIn(0f, 1f)
        } else {
            info.progress.toFloat().coerceIn(0f, 1f)
        }
    } ?: 0f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = "我的")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "钱包",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    if (loading && wallet == null) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            text = "${formatNumber((wallet?.vCoins ?: 0).toLong())} V币",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                        )
                    }
                }

                wallet?.items?.takeIf { it.isNotEmpty() }?.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = item.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            text = "×${item.quantity}",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = scheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "签到",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    if (level != null) {
                        Text(
                            text = if (checkedIn) "今日已签到" else "今日未签",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (checkedIn) scheme.primary else scheme.onSurfaceVariant,
                        )
                    }
                }

                if (level != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (checkedIn) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = scheme.primary,
                            )
                        }
                        Text(
                            text = "Lv.${level.level} · 连续 ${level.streakDays} 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${formatNumber(level.currentLevelExp.toLong())} / ${formatNumber(level.nextLevelExp.toLong())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        drawStopIndicator = {},
                    )

                    if (!checkedIn) {
                        Button(
                            onClick = onCheckin,
                            enabled = !checkinBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (checkinBusy) "签到中…" else "每日签到")
                        }
                    }
                }

                notice?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onOpenSettings) {
                        Text("账号设置")
                    }
                    onOpenWallet?.let { action ->
                        TextButton(onClick = action) {
                            Text("去钱包")
                        }
                    }
                }
            }
        }
    }
}
