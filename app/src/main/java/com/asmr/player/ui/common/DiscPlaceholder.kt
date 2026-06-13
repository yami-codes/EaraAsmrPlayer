package com.asmr.player.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.asmr.player.R
import com.asmr.player.ui.theme.AsmrTheme

@Composable
fun DiscPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 6
) {
    val placeholderRes = if (AsmrTheme.colorScheme.isDark) {
        R.drawable.image_empty_dark
    } else {
        R.drawable.image_empty_light
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
    ) {
        Image(
            painter = painterResource(placeholderRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

