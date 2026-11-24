package com.mymovie.log.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mymovie.log.presentation.ui.theme.PopcornColors

/**
 * Two or more pill options inside a white rounded track; the selected one is filled with butter.
 * Used for the library tabs ([Role.Tab]) and the watched / wishlist choice in the record editor
 * ([Role.RadioButton]).
 */
@Composable
fun <T> PopcornToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    role: Role,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(PopcornColors.Surface)
            .border(1.dp, PopcornColors.Outline, CircleShape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PopcornColors.Butter else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = role,
                        onClick = { onSelect(option) }
                    )
                    .testTag(PopcornToggleTestTags.option(option.toString())),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PopcornColors.Ink else PopcornColors.InkMuted
                )
            }
        }
    }
}

object PopcornToggleTestTags {
    fun option(key: String) = "popcorn_toggle_$key"
}
