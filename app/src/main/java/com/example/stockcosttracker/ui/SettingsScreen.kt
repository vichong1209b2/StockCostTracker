package com.example.stockcosttracker.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class DcaFeeMode {
    FIXED,
    RATE_WITH_MINIMUM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: PortfolioViewModel = viewModel(
        factory = PortfolioViewModel.provideFactory(context.applicationContext as Application)
    )

    var regularFeeRate by rememberSaveable { mutableStateOf("0.001425") }
    var regularMinimumFee by rememberSaveable { mutableStateOf("20") }

    var dcaFeeMode by rememberSaveable { mutableStateOf(DcaFeeMode.FIXED) }
    var dcaFixedFee by rememberSaveable { mutableStateOf("1") }
    var dcaFeeRate by rememberSaveable { mutableStateOf("0.001425") }
    var dcaMinimumFee by rememberSaveable { mutableStateOf("1") }

    var autoUpdateNameOnLaunch by rememberSaveable { mutableStateOf(true) }
    var useEstimatedSellTax by rememberSaveable { mutableStateOf(true) }
    var autoSyncDailyClosingOnLaunch by rememberSaveable {
        mutableStateOf(viewModel.isAutoAfterHoursSyncEnabled())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "一般交易費率",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "常見券商做法為手續費率 0.1425%，且最低手續費 20 元。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = regularFeeRate,
                        onValueChange = { regularFeeRate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("手續費率，例如 0.001425") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = regularMinimumFee,
                        onValueChange = { regularMinimumFee = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("最低手續費，例如 20") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "定期定額買入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "若你目前在新光證券定期定額實際每筆只被扣 1 元，建議先用固定手續費模式。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FeeModeOptionRow(
                        title = "固定手續費",
                        selected = dcaFeeMode == DcaFeeMode.FIXED,
                        onClick = { dcaFeeMode = DcaFeeMode.FIXED }
                    )

                    FeeModeOptionRow(
                        title = "費率 + 最低手續費",
                        selected = dcaFeeMode == DcaFeeMode.RATE_WITH_MINIMUM,
                        onClick = { dcaFeeMode = DcaFeeMode.RATE_WITH_MINIMUM }
                    )

                    if (dcaFeeMode == DcaFeeMode.FIXED) {
                        OutlinedTextField(
                            value = dcaFixedFee,
                            onValueChange = { dcaFixedFee = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("固定手續費，例如 1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    } else {
                        OutlinedTextField(
                            value = dcaFeeRate,
                            onValueChange = { dcaFeeRate = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("手續費率，例如 0.001425") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = dcaMinimumFee,
                            onValueChange = { dcaMinimumFee = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("最低手續費，例如 1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "其他設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    SettingSwitchRow(
                        title = "啟動時更新股票名稱資料",
                        checked = autoUpdateNameOnLaunch,
                        onCheckedChange = { autoUpdateNameOnLaunch = it }
                    )

                    HorizontalDivider()

                    SettingSwitchRow(
                        title = "估算賣出時證交稅",
                        checked = useEstimatedSellTax,
                        onCheckedChange = { useEstimatedSellTax = it }
                    )

                    HorizontalDivider()

                    SettingSwitchRow(
                        title = "自動取得每日盤後股價",
                        checked = autoSyncDailyClosingOnLaunch,
                        onCheckedChange = {
                            autoSyncDailyClosingOnLaunch = it
                            viewModel.setAutoAfterHoursSyncEnabled(it)
                        }
                    )

                    Text(
                        text = "開啟後，每次打開 app 若已盤後，會自動抓取最新可用的每日盤後股價，更新現價並記錄每日總損益；若仍在盤中則不自動執行。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = { viewModel.refreshStockInfos() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("更新股票名稱資料")
                    }

                    Button(
                        onClick = { onNavigateBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("儲存")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeModeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        TextButton(onClick = onClick) {
            Text(title)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}