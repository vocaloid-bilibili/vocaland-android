package xyz.lisfox.vocaland.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.data.model.HeroSlideDto
import xyz.lisfox.vocaland.ui.components.MotionDurations
import xyz.lisfox.vocaland.ui.components.NetworkImage

@Composable
fun HeroCarousel(
    slides: List<HeroSlideDto>,
    onClick: (HeroSlideDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (slides.isEmpty()) {
        EmptyHero(modifier)
        return
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // 用户手动滑动时暂停自动轮播，松手后继续
    // 注意：只监听 isScrollInProgress 这种离散状态，避免依赖每帧变化的 offset
    // 导致 LaunchedEffect 反复重启
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(slides.size, lifecycleState) {
        if (slides.size <= 1 || lifecycleState != Lifecycle.State.RESUMED) {
            return@LaunchedEffect
        }
        snapshotFlow { pagerState.isScrollInProgress }
            .collectLatest { scrolling ->
                if (scrolling) return@collectLatest
                while (currentCoroutineContext().isActive) {
                    delay(MotionDurations.CAROUSEL_INTERVAL_MS)
                    if (pagerState.isScrollInProgress) break
                    val next = (pagerState.currentPage + 1) % slides.size
                    pagerState.animateScrollToPage(next)
                }
            }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Box {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) { page ->
                val slide = slides[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onClick(slide) },
                ) {
                    NetworkImage(
                        url = slide.imageUrl,
                        contentDescription = slide.caption,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                    ),
                                ),
                            ),
                    )
                    Text(
                        text = slide.caption,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 96.dp, bottom = 14.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(slides.size) { index ->
                    val selected = pagerState.currentPage == index
                    val pageDescription = stringResource(
                        R.string.carousel_page_format,
                        index + 1,
                        slides.size,
                    )
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 16.dp else 6.dp,
                        animationSpec = tween(
                            durationMillis = MotionDurations.MEDIUM,
                            easing = FastOutSlowInEasing,
                        ),
                        label = "heroIndicatorWidth",
                    )
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.45f,
                        animationSpec = tween(MotionDurations.MEDIUM),
                        label = "heroIndicatorAlpha",
                    )
                    // 触摸热区保持 >= 32dp，视觉圆点仍为 6dp
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp),
                            ) {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                            .semantics { contentDescription = pageDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = dotAlpha)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHero(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.tertiaryContainer),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "VocaLand",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    text = "术力口数据库",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}
