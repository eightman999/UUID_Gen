package com.furinlab.eightman.uuid_gen.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.furinlab.eightman.uuid_gen.ui.screens.GeneratorScreen
import com.furinlab.eightman.uuid_gen.ui.screens.HistoryScreen
import com.furinlab.eightman.uuid_gen.ui.screens.SettingsScreen
import com.furinlab.eightman.uuid_gen.ui.screens.StoreScreen

@Composable
fun UuidApp(viewModel: UuidViewModel) {
    val storeState by viewModel.storeState.collectAsStateWithLifecycle()
    val preferencesState by viewModel.preferencesState.collectAsStateWithLifecycle()
    val generatedState by viewModel.generated.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.Generator) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppDestination.values().forEach { dest ->
                    NavigationBarItem(
                        selected = destination == dest,
                        onClick = { destination = dest },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (destination) {
            AppDestination.Generator -> GeneratorScreen(
                modifier = Modifier.padding(innerPadding),
                storeState = storeState,
                preferences = preferencesState,
                generated = generatedState,
                onGenerate = viewModel::generate,
                onSave = viewModel::saveCurrent,
                onUpdateFormat = viewModel::updateFormat,
                onUpdateDefaultVersion = viewModel::updateDefaultVersion
            )
            AppDestination.History -> HistoryScreen(
                modifier = Modifier.padding(innerPadding),
                storeState = storeState,
                onDelete = viewModel::delete,
                onClear = viewModel::clearAll,
                onAddBonus = viewModel::addBonusSlot
            )
            AppDestination.Store -> StoreScreen(
                modifier = Modifier.padding(innerPadding),
                storeState = storeState,
                onTogglePro = viewModel::togglePro,
                onAddBonus = viewModel::addBonusSlot
            )
            AppDestination.Settings -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
                preferences = preferencesState,
                onUpdateDefaultVersion = viewModel::updateDefaultVersion,
                onUpdateFormat = viewModel::updateFormat
            )
        }
    }
}

enum class AppDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Generator("生成", Icons.Filled.AutoAwesome),
    History("履歴", Icons.Filled.History),
    Store("ストア", Icons.Filled.Store),
    Settings("設定", Icons.Filled.Settings)
}
