package com.example.cloudide.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cloudide.ui.theme.CardShape
import com.example.cloudide.ui.theme.CardWhite
import com.example.cloudide.ui.theme.ShadowSoft

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = CardWhite,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shadowModifier = modifier.shadow(
        elevation = 6.dp,
        shape = CardShape,
        ambientColor = ShadowSoft,
        spotColor = ShadowSoft
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = shadowModifier,
            shape = CardShape,
            color = containerColor
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = shadowModifier,
            shape = CardShape,
            color = containerColor
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}