package com.furinlab.eightman.uuid_gen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.furinlab.eightman.uuid_gen.data.UuidRepository
import com.furinlab.eightman.uuid_gen.ui.formatTime

@Composable
fun StoreScreen(
    modifier: Modifier = Modifier,
    storeState: UuidRepository.StoreState,
    onTogglePro: () -> Unit,
    onAddBonus: () -> Unit
) {
    Scaffold(modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("UUID Pro", style = MaterialTheme.typography.titleMedium)
                        Text("保存数無制限、広告非表示（予定）")
                        Button(onClick = onTogglePro) {
                            Text(if (storeState.entitlement.isPro) "Pro を解除 (開発用)" else "Pro を購入 (テスト)")
                        }
                        Text(
                            "StoreKit / Billing 実装まではローカルで状態を切り替えます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ボーナス枠", style = MaterialTheme.typography.titleMedium)
                        if (storeState.bonusSlots.isEmpty()) {
                            Text("ボーナス枠はありません")
                        } else {
                            storeState.bonusSlots.forEach { slot ->
                                Text("有効期限: ${formatTime(slot.expiresAt)}")
                            }
                        }
                        OutlinedButton(
                            onClick = onAddBonus,
                            enabled = storeState.bonusSlots.size < com.furinlab.eightman.uuid_gen.domain.LimitConstants.MAX_BONUS
                        ) {
                            Text("広告視聴をシミュレート")
                        }
                    }
                }
            }
        }
    }
}
