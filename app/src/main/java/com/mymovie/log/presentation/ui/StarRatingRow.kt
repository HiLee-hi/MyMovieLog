package com.mymovie.log.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun StarRatingRow(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            val filled = rating >= star
            val halfFilled = !filled && rating >= star - 0.5f
            Icon(
                imageVector = when {
                    filled -> Icons.Default.Star
                    halfFilled -> Icons.Default.StarHalf
                    else -> Icons.Outlined.StarBorder
                },
                contentDescription = "$star 점",
                tint = if (filled || halfFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onRatingChange(if (rating == star.toFloat()) star - 0.5f else star.toFloat())
                    }
            )
        }
    }
}
