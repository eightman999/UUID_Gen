package com.furinlab.eightman.uuid_gen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.furinlab.eightman.uuid_gen.data.UuidRepository
import com.furinlab.eightman.uuid_gen.domain.UuidRecord
import com.furinlab.eightman.uuid_gen.ui.formatDateTime
import com.furinlab.eightman.uuid_gen.ui.formatTime
import com.furinlab.eightman.uuid_gen.ui.shareText

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    storeState: UuidRepository.StoreState,
    onDelete: (UuidRecord) -> Unit,
    onClear: () -> Unit,
    onAddBonus: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedRecord by rememberSaveable { mutableStateOf<UuidRecord?>(null) }

    Scaffold(modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(
                    storeState = storeState,
                    onClear = onClear,
                    onAddBonus = onAddBonus
                )
            }
            if (storeState.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("保存された UUID はありません")
                    }
                }
            } else {
                items(storeState.items, key = { it.id }) { record ->
                    HistoryItem(
                        record = record,
                        onCopy = {
                            clipboard.setText(AnnotatedString(record.formattedValue))
                            Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                        },
                        onShare = { shareText(context, record.formattedValue) },
                        onDelete = { onDelete(record) },
                        onDetail = { selectedRecord = record }
                    )
                }
            }
        }
    }

    selectedRecord?.let { record ->
        HistoryDetailDialog(record = record, onDismiss = { selectedRecord = null })
    }
}

@Composable
private fun SummaryCard(
    storeState: UuidRepository.StoreState,
    onClear: () -> Unit,
    onAddBonus: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("保存状況", style = MaterialTheme.typography.titleMedium)
            if (storeState.entitlement.isPro) {
                Text("保存数: ${storeState.items.size}")
                Text("保存上限: 無制限")
            } else {
                Text("保存数: ${storeState.items.size} / ${storeState.limitState.capacity}")
                Text("ボーナス枠: ${storeState.bonusSlots.size} / ${com.furinlab.eightman.uuid_gen.domain.LimitConstants.MAX_BONUS}")
            }
            if (storeState.bonusSlots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    storeState.bonusSlots.forEach { slot ->
                        Text(
                            text = "ボーナス枠 有効期限: ${formatTime(slot.expiresAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClear,
                    enabled = storeState.items.isNotEmpty()
                ) {
                    Text("全削除")
                }
                if (!storeState.entitlement.isPro) {
                    Button(
                        onClick = onAddBonus,
                        enabled = storeState.bonusSlots.size < com.furinlab.eightman.uuid_gen.domain.LimitConstants.MAX_BONUS
                    ) {
                        Text("広告で +1")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    record: UuidRecord,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDetail: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(record.formattedValue, fontFamily = FontFamily.Monospace)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(record.version.raw.uppercase(), style = MaterialTheme.typography.bodySmall)
                Text(formatDateTime(record.createdAt), style = MaterialTheme.typography.bodySmall)
            }
            record.label?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "コピー")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "共有")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                }
                IconButton(onClick = onDetail) {
                    Icon(Icons.Default.Info, contentDescription = "詳細")
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailDialog(record: UuidRecord, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
        title = { Text("UUID 詳細") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "UUID", value = record.formattedValue)
                DetailRow(label = "バージョン", value = record.version.raw.uppercase())
                DetailRow(label = "生成時刻", value = formatDateTime(record.createdAt))
                record.namespace?.let { DetailRow(label = "Namespace", value = it) }
                record.name?.let { DetailRow(label = "Name", value = it) }
                record.label?.let { DetailRow(label = "ラベル", value = it) }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
