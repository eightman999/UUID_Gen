package com.furinlab.eightman.uuid_gen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.furinlab.eightman.uuid_gen.domain.model.UuidFormatOptions
import com.furinlab.eightman.uuid_gen.domain.model.UuidItem
import com.furinlab.eightman.uuid_gen.domain.model.UuidVersion
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

@Composable
fun MainScreen(
    uiState: MainUiState,
    onVersionSelected: (UuidVersion) -> Unit,
    onNamespaceChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onFormatChanged: (UuidFormatOptions) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDelete: (String) -> Unit,
    onTogglePro: () -> Unit,
    onGrantBonus: () -> Unit,
    onDismissGeneratedDialog: () -> Unit,
    onDismissError: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onGenerate) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "生成")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "UUID Generator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )

            VersionSelector(uiState.selectedVersion, onVersionSelected)

            if (uiState.selectedVersion == UuidVersion.V5) {
                NamespaceInput(
                    namespace = uiState.namespaceInput,
                    name = uiState.nameInput,
                    onNamespaceChanged = onNamespaceChanged,
                    onNameChanged = onNameChanged,
                )
            }

            FormatOptionsSection(uiState.formatOptions, onFormatChanged)

            ActionButtons(
                onSave = onSave,
                onClear = onClear,
                canSave = uiState.canSave && uiState.generatedValue != null,
            )

            GeneratedValueCard(uiState.generatedValue, uiState.errorMessage)

            LimitStatusSection(uiState)

            ControlRow(
                onTogglePro = onTogglePro,
                onGrantBonus = onGrantBonus,
            )

            Divider()

            HistoryList(
                modifier = Modifier.weight(1f),
                items = uiState.history,
                onDelete = onDelete,
            )
        }

        if (uiState.showGeneratedDialog && uiState.generatedValue != null) {
            AlertDialog(
                onDismissRequest = onDismissGeneratedDialog,
                confirmButton = {
                    TextButton(onClick = onDismissGeneratedDialog) {
                        Text("閉じる")
                    }
                },
                title = { Text("生成したUUID") },
                text = {
                    Text(
                        text = uiState.generatedValue,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                },
            )
        }

        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text("OK")
                    }
                },
                title = { Text("警告") },
                text = { Text(uiState.errorMessage) },
            )
        }
    }
}

@Composable
private fun VersionSelector(
    selected: UuidVersion,
    onVersionSelected: (UuidVersion) -> Unit,
) {
    val versions = remember { UuidVersion.values() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "バージョン", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            versions.forEach { version ->
                FilterChip(
                    selected = selected == version,
                    onClick = { onVersionSelected(version) },
                    label = { Text(text = version.displayName.uppercase()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun NamespaceInput(
    namespace: String,
    name: String,
    onNamespaceChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "v5 用入力", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = namespace,
            onValueChange = onNamespaceChanged,
            label = { Text("Namespace UUID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun FormatOptionsSection(
    options: UuidFormatOptions,
    onFormatChanged: (UuidFormatOptions) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "整形オプション", style = MaterialTheme.typography.titleMedium)
        FormatToggle(
            label = "ハイフン",
            checked = options.hyphenated,
            onCheckedChange = { onFormatChanged(options.copy(hyphenated = it)) },
        )
        FormatToggle(
            label = "大文字",
            checked = options.uppercase,
            onCheckedChange = { onFormatChanged(options.copy(uppercase = it)) },
        )
        FormatToggle(
            label = "波括弧 { }",
            checked = options.braces,
            onCheckedChange = { onFormatChanged(options.copy(braces = it)) },
        )
    }
}

@Composable
private fun FormatToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ActionButtons(
    onSave: () -> Unit,
    onClear: () -> Unit,
    canSave: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onSave, enabled = canSave) {
            Text(text = "保存")
        }
        OutlinedButton(onClick = onClear) {
            Text(text = "クリア")
        }
    }
}

@Composable
private fun GeneratedValueCard(
    generatedValue: String?,
    errorMessage: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "生成結果", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            when {
                errorMessage != null -> Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                generatedValue != null -> Text(
                    text = generatedValue,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                else -> Text(text = "未生成")
            }
        }
    }
}

@Composable
private fun LimitStatusSection(uiState: MainUiState) {
    val limitState = uiState.limitState
    val capacityText = if (limitState.isPro) "∞" else limitState.capacity().toString()
    val bonusText = if (limitState.bonusActive > 0) " (+${limitState.bonusActive})" else ""
    val status = "${uiState.history.size} / $capacityText$bonusText"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "保存上限", style = MaterialTheme.typography.titleMedium)
        Text(text = status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ControlRow(
    onTogglePro: () -> Unit,
    onGrantBonus: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onTogglePro) {
            Text("Pro 切替")
        }
        OutlinedButton(onClick = onGrantBonus) {
            Text("ボーナス +1")
        }
    }
}

@Composable
private fun HistoryList(
    modifier: Modifier = Modifier,
    items: List<UuidItem>,
    onDelete: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "保存済み UUID", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(text = "まだ保存されていません", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryItem(item = item, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    item: UuidItem,
    onDelete: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.format.apply(item.value),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.version.displayName.uppercase()} ・ ${item.createdAt.formatAsDateTime()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { onDelete(item.id) }) {
                    Text("削除")
                }
            }
        }
    }
}

private fun Long.formatAsDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(HistoryDateFormatter)
}
