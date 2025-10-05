package com.furinlab.eightman.uuid_gen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.furinlab.eightman.uuid_gen.data.UserPreferencesRepository
import com.furinlab.eightman.uuid_gen.domain.FormatOption
import com.furinlab.eightman.uuid_gen.domain.UuidVersion

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserPreferencesRepository.PreferencesState,
    onUpdateDefaultVersion: (UuidVersion) -> Unit,
    onUpdateFormat: (FormatOption, Boolean) -> Unit
) {
    val formatOptions = preferences.formatOptions
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("既定の生成設定", style = MaterialTheme.typography.titleMedium)
                    UuidVersion.values().forEach { version ->
                        VersionOption(
                            version = version,
                            selected = version == preferences.defaultVersion,
                            onClick = { onUpdateDefaultVersion(version) }
                        )
                        Divider()
                    }
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("整形オプション", style = MaterialTheme.typography.titleMedium)
                    FormatToggle(label = "ハイフンを含める", checked = formatOptions.contains(FormatOption.HYPHEN)) {
                        onUpdateFormat(FormatOption.HYPHEN, it)
                    }
                    FormatToggle(label = "大文字にする", checked = formatOptions.contains(FormatOption.UPPERCASE)) {
                        onUpdateFormat(FormatOption.UPPERCASE, it)
                    }
                    FormatToggle(label = "{} を付与", checked = formatOptions.contains(FormatOption.BRACES)) {
                        onUpdateFormat(FormatOption.BRACES, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionOption(version: UuidVersion, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(version.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                version.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormatToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
