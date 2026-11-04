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

@Composable
fun PagerRow(
    page: Int,
    totalPages: Int,
    total: Int,
    loading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "首",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = page > 1 && !loading,
        ) {
            Text("上一页")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "第 $page / $totalPages 页",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "共 $total $unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(
            onClick = onNext,
            enabled = page < totalPages && !loading,
        ) {
            Text("下一页")
        }
    }
}
