package xyz.lisfox.vocaland.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.util.formatNumber

@Composable
fun PagerRow(
    page: Int,
    totalPages: Int,
    loading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    total: Int? = null,
    unit: String? = null,
) {
    if (totalPages <= 1) return
    val resolvedUnit = unit ?: stringResource(R.string.pager_unit_default)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = page > 1 && !loading,
        ) {
            Text(stringResource(R.string.action_previous))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.pager_page_format, page, totalPages),
                style = MaterialTheme.typography.labelMedium,
            )
            if (total != null) {
                Text(
                    text = stringResource(
                        R.string.pager_total_format,
                        formatNumber(total.toLong()),
                        resolvedUnit,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TextButton(
            onClick = onNext,
            enabled = page < totalPages && !loading,
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}
