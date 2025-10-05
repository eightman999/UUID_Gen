package com.furinlab.eightman.uuid_gen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.furinlab.eightman.uuid_gen.ui.MainScreen
import com.furinlab.eightman.uuid_gen.ui.MainViewModel
import com.furinlab.eightman.uuid_gen.ui.theme.UuidGenTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UuidGenTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    uiState = uiState,
                    onVersionSelected = viewModel::onVersionSelected,
                    onNamespaceChanged = viewModel::onNamespaceChanged,
                    onNameChanged = viewModel::onNameChanged,
                    onFormatChanged = viewModel::onFormatChanged,
                    onGenerate = viewModel::generateUuid,
                    onSave = viewModel::saveGenerated,
                    onClear = viewModel::clearGenerated,
                    onDelete = viewModel::deleteItem,
                    onTogglePro = viewModel::togglePro,
                    onGrantBonus = viewModel::grantBonusSlot,
                )
            }
        }
    }
}
