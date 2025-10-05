package com.furinlab.eightman.uuid_gen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.furinlab.eightman.uuid_gen.data.UuidRepository
import com.furinlab.eightman.uuid_gen.data.UserPreferencesRepository
import com.furinlab.eightman.uuid_gen.domain.FormatOption
import com.furinlab.eightman.uuid_gen.domain.UuidVersion
import com.furinlab.eightman.uuid_gen.ui.UuidViewModel
import com.furinlab.eightman.uuid_gen.ui.formatDateTime
import com.furinlab.eightman.uuid_gen.ui.formatTime
import com.furinlab.eightman.uuid_gen.ui.shareText

@Composable
fun GeneratorScreen(
    modifier: Modifier = Modifier,
    storeState: UuidRepository.StoreState,
    preferences: UserPreferencesRepository.PreferencesState,
    generated: UuidViewModel.GeneratedUiState?,
    onGenerate: (UuidVersion, String?, String?) -> Unit,
    onSave: (String?) -> Unit,
    onUpdateFormat: (FormatOption, Boolean) -> Unit,
    onUpdateDefaultVersion: (UuidVersion) -> Unit
) {
    var selectedVersion by rememberSaveable { mutableStateOf(preferences.defaultVersion) }
    var namespace by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(preferences.defaultVersion) {
        selectedVersion = preferences.defaultVersion
    }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val formatOptions = preferences.formatOptions

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onGenerate(
                    selectedVersion,
                    namespace.trim().ifBlank { null },
                    name.trim().ifBlank { null }
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "UUIDを生成")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            VersionSection(
                selected = selectedVersion,
                onSelected = {
                    selectedVersion = it
                    onUpdateDefaultVersion(it)
                }
            )

            if (selectedVersion == UuidVersion.V5) {
                NamespaceSection(
                    namespace = namespace,
                    onNamespaceChange = { namespace = it },
                    name = name,
                    onNameChange = { name = it }
                )
            }

            FormatSection(formatOptions = formatOptions, onUpdate = onUpdateFormat)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("ラベル (任意)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            generated?.let { result ->
                ResultSection(
                    generated = result,
                    storeState = storeState,
                    onCopy = {
                        clipboard.setText(AnnotatedString(result.formattedValue))
                        Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        shareText(context, result.formattedValue)
                    },
                    onSave = {
                        onSave(label.trim().ifBlank { null })
                        if (label.isNotEmpty()) {
                            label = ""
                        }
                    }
                )
            }

            CapacitySection(storeState = storeState)
        }
    }
}

@Composable
private fun VersionSection(selected: UuidVersion, onSelected: (UuidVersion) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("バージョン", style = MaterialTheme.typography.titleMedium)
        UuidVersion.values().forEach { version ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = version == selected,
                        onClick = { onSelected(version) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = version == selected,
                    onClick = { onSelected(version) }
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(version.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        version.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Divider()
        }
    }
}

@Composable
private fun NamespaceSection(
    namespace: String,
    onNamespaceChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("名前空間 (UUID v5)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = namespace,
            onValueChange = onNamespaceChange,
            label = { Text("Namespace UUID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
    }
}

@Composable
private fun FormatSection(
    formatOptions: com.furinlab.eightman.uuid_gen.domain.FormatOptions,
    onUpdate: (FormatOption, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("整形オプション", style = MaterialTheme.typography.titleMedium)
        FormatToggleRow("ハイフンを含める", formatOptions.contains(FormatOption.HYPHEN)) {
            onUpdate(FormatOption.HYPHEN, it)
        }
        FormatToggleRow("大文字にする", formatOptions.contains(FormatOption.UPPERCASE)) {
            onUpdate(FormatOption.UPPERCASE, it)
        }
        FormatToggleRow("{} を付与", formatOptions.contains(FormatOption.BRACES)) {
            onUpdate(FormatOption.BRACES, it)
        }
    }
}

@Composable
private fun FormatToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ResultSection(
    generated: UuidViewModel.GeneratedUiState,
    storeState: UuidRepository.StoreState,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = generated.formattedValue,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("バージョン: ${generated.generated.version.raw.uppercase()}")
                Text("生成時刻: ${formatDateTime(generated.generated.createdAt)}")
                generated.generated.namespace?.let {
                    Text("Namespace: $it")
                }
                generated.generated.name?.let {
                    Text("Name: $it")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "コピー")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "共有")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onSave, enabled = storeState.canAddMore) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun CapacitySection(storeState: UuidRepository.StoreState) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("保存可能数", style = MaterialTheme.typography.titleMedium)
            if (storeState.entitlement.isPro) {
                Text("保存上限: 無制限")
                Text("保存数: ${storeState.items.size}")
            } else {
                Text("保存数: ${storeState.items.size} / ${storeState.limitState.capacity}")
                if (storeState.bonusSlots.isNotEmpty()) {
                    Text("ボーナス枠: ${storeState.bonusSlots.size} / ${com.furinlab.eightman.uuid_gen.domain.LimitConstants.MAX_BONUS}")
                }
            }
            if (storeState.bonusSlots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                storeState.bonusSlots.forEach { slot ->
                    Text(
                        text = "ボーナス枠 有効期限: ${formatTime(slot.expiresAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

