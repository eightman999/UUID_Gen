package com.furinlab.eightman.uuid_gen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.furinlab.eightman.uuid_gen.data.AppContainer
import com.furinlab.eightman.uuid_gen.ui.UuidApp
import com.furinlab.eightman.uuid_gen.ui.UuidViewModel
import com.furinlab.eightman.uuid_gen.ui.UuidViewModelFactory
import com.furinlab.eightman.uuid_gen.ui.theme.UUIDGenTheme

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer by lazy { AppContainer(applicationContext) }

    private val viewModel: UuidViewModel by viewModels {
        UuidViewModelFactory(appContainer.uuidRepository, appContainer.preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UUIDGenTheme {
                UuidApp(viewModel = viewModel)
            }
        }
    }
}
