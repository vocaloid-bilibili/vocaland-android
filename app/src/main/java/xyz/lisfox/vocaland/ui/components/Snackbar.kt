package xyz.lisfox.vocaland.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局 Snackbar 宿主，由 [xyz.lisfox.vocaland.ui.VocaLandApp] 在根部提供。
 *
 * 轻量的一次性提示（如「评论已发布」）走 Snackbar，不要塞进页面内容里，
 * 否则会一直占着版面且无法自动消失。
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }
