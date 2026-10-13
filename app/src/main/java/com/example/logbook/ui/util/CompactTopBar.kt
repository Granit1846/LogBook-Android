package com.example.logbook.ui.util

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Компактный TopAppBar высотой 32dp + отступ под системный статус-бар.
 *
 * Параметры:
 *   title             — заголовок
 *   navigationIcon    — иконка навигации (обычно «назад»), опционально
 *   onNavigationClick — обработчик клика по иконке навигации
 *   actions           — содержимое правой части
 */
@Composable
fun CompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(
                    onClick = onNavigationClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = tr(R.string.msg_back_f6dab07),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = if (navigationIcon != null) 8.dp else 12.dp)
                    .weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}