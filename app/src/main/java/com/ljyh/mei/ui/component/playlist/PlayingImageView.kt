package com.ljyh.mei.ui.component.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun PlayingImageView(
    imageUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(context, imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageUrl)
            .crossfade(true)
            .build()
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 使用Coil 3加载网络图片
        AsyncImage(
            model = request,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 如果正在播放，则显示动态音波动画
        if (isPlaying) {
            AnimatedSoundWave(
                modifier = Modifier.size(16.dp) // 控制动画的大小
            )
        }
    }
}