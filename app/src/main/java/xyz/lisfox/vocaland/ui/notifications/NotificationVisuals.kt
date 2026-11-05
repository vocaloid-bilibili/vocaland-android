package xyz.lisfox.vocaland.ui.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.ui.graphics.vector.ImageVector
import xyz.lisfox.vocaland.data.notification.NotificationKind

fun notificationIcon(kind: NotificationKind): ImageVector = when (kind) {
    NotificationKind.Reply -> Icons.Filled.Message
    NotificationKind.Mention -> Icons.Filled.AlternateEmail
    NotificationKind.Like -> Icons.Filled.Favorite
    NotificationKind.WallComment -> Icons.Filled.Home
    NotificationKind.ThreadUpdate -> Icons.Filled.Forum
    NotificationKind.TargetUpdate -> Icons.Filled.ChatBubble
}
