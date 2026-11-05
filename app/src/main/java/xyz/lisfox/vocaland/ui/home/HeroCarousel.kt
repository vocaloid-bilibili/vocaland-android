package xyz.lisfox.vocaland.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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

    // 用户手动滑动 / 页面被按下时暂停自动轮播，松手后继续
    val userInteracting by remember {
        derivedStateOf {
            pagerState.isScrollInProgress || pagerState.currentPageOffsetFraction != 0f
        }
    }
    LaunchedEffect(slides.size, userInteracting) {
        if (slides.size <= 1 || userInteracting) return@LaunchedEffect
        while (true) {
            delay(5_000)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
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
                                        Color.Black.copy(alpha = 0.72f),
                                    ),
                                ),
                            ),
                    )
                    Text(
                        text = slide.caption,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 64.dp, bottom = 14.dp),
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
