// PortfolioScreen
// V1.1.1
package com.example.stockcosttracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcosttracker.data.local.entity.DailyPortfolioSnapshotEntity
import com.example.stockcosttracker.domain.model.PortfolioSummary
import com.example.stockcosttracker.domain.model.StockInfo
import com.example.stockcosttracker.domain.model.StockTransaction
import com.example.stockcosttracker.domain.model.TransactionType
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu

private val currencyFormat = DecimalFormat("#,##0.00")
private val priceFormat = DecimalFormat("#,##0.00")
private val integerFormat = DecimalFormat("#,##0")
private val lotFormat = DecimalFormat("#,##0.###")
private val percentFormat = DecimalFormat("#,##0.##")

private val SectionInputColor = Color(0xFFFFFFFF)
private val SectionSummaryColor = Color(0xFFEAF6F8)
private val SectionHoldingColor = Color(0xFFF2F8F3)
private val SectionTransactionColor = Color(0xFFF7F2F5)

private val PositiveRed = Color(0xFFC43D4B)
private val PositiveRedBg = Color(0xFFC43D4B)
private val NegativeGreen = Color(0xFF1E8A5A)
private val NegativeGreenBg = Color(0xFF1E8A5A)
private val NeutralGrayBg = Color(0xFFE9EEF1)
private val NeutralGrayText = Color(0xFF52616A)
private val WhiteText = Color.White

fun todayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

private fun formatMillisToDate(millis: Long): String {
    val localDate = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun formatIntValue(value: Double): String = integerFormat.format(value)
private fun formatIntMoney(value: Double): String = integerFormat.format(value)
private fun formatPriceValue(value: Double): String = priceFormat.format(value)
private fun formatHoldingDisplay(shares: Double): String = "${formatIntValue(shares)}股"

private fun formatSignedIntAmount(amount: Double): String {
    val sign = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    return "$sign${formatIntMoney(abs(amount))}"
}

private fun formatSignedPriceDiff(amount: Double): String {
    val sign = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    return "$sign${formatPriceValue(abs(amount))}"
}

private fun formatCompactNumber(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 100_000_000 -> "${percentFormat.format(value / 100_000_000)}億"
        absValue >= 10_000 -> "${percentFormat.format(value / 10_000)}萬"
        else -> integerFormat.format(value)
    }
}

private fun formatCompactSignedMoney(value: Double): String {
    val sign = when {
        value > 0 -> "+"
        value < 0 -> "-"
        else -> ""
    }
    return "$sign${formatCompactNumber(abs(value))}"
}

fun quantityToLotInput(
    quantityInput: String,
    quantityUnit: TradeQuantityUnit
): String {
    val quantity = quantityInput.trim().toDoubleOrNull() ?: return quantityInput
    return when (quantityUnit) {
        TradeQuantityUnit.LOT -> quantityInput.trim()
        TradeQuantityUnit.SHARE -> (quantity / 1000.0).toString()
    }
}

enum class TradeQuantityUnit(val label: String) {
    LOT("張"),
    SHARE("股")
}

enum class PriceSourceLabel(val label: String) {
    INTRADAY("盤中即時價"),
    CLOSING("最近收盤價"),
    CACHED("快取價格"),
    UNKNOWN("尚未取得價格")
}

private enum class MiniChipTone {
    HOLDING,
    COST,
    PRICE,
    PROFIT,
    NEUTRAL
}

private fun detectPriceSource(
    currentPrice: Double?,
    lastQuoteTime: String?
): PriceSourceLabel {
    if (currentPrice == null || lastQuoteTime.isNullOrBlank()) {
        return PriceSourceLabel.UNKNOWN
    }

    return try {
        if (lastQuoteTime.length <= 10) {
            PriceSourceLabel.CLOSING
        } else {
            val parsed = runCatching { LocalDateTime.parse(lastQuoteTime) }.getOrNull()
            if (parsed != null && isTradingHours(parsed.toLocalDate(), parsed.toLocalTime())) {
                PriceSourceLabel.INTRADAY
            } else {
                PriceSourceLabel.CACHED
            }
        }
    } catch (_: Exception) {
        PriceSourceLabel.CACHED
    }
}

private fun isTradingHours(date: LocalDate, time: LocalTime): Boolean {
    val isWeekday = date.dayOfWeek.value in 1..5
    val tradingStart = LocalTime.of(9, 0)
    val tradingEnd = LocalTime.of(13, 30)
    return isWeekday && time >= tradingStart && time <= tradingEnd
}

private fun formatPriceUpdatedAt(lastQuoteTime: String?): String {
    if (lastQuoteTime.isNullOrBlank()) return "—"

    return try {
        if (lastQuoteTime.length <= 10) {
            lastQuoteTime
        } else {
            val parsed = LocalDateTime.parse(lastQuoteTime)
            parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
    } catch (_: Exception) {
        lastQuoteTime
    }
}

fun buildSuggestions(
    input: String,
    stockInfos: List<StockInfo>
): List<StockInfo> {
    val keyword = input.trim()
    if (keyword.length < 2) return emptyList()
    return stockInfos.asSequence()
        .filter { it.stockCode.startsWith(keyword) || it.stockName.contains(keyword) }
        .take(5)
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onOpenAddTransaction: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: PortfolioViewModel = viewModel(
        factory = PortfolioViewModel.provideFactory(application)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeSummaries = remember(uiState.summaries) {
        uiState.summaries.filter { it.holdingShares > 0.0 }
    }

    val expandedSummaryMap = remember { mutableStateMapOf<String, Boolean>() }


    var trendDays by rememberSaveable { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        viewModel.onAppLaunch()
        viewModel.refreshStockInfos()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("股票損益追蹤") },
                actions = {
                    TextButton(onClick = onOpenAddTransaction) {
                        Text("新增交易")
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text("設定")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            item {
                PortfolioOverviewCard(
                    summaries = activeSummaries
                )
            }

            item {
                ActionCard(
                    isRefreshing = uiState.isRefreshingQuotes,
                    trackedCount = activeSummaries.size,
                    onRefresh = viewModel::refreshQuotes
                )
            }

            item {
                PortfolioTrendCard(
                    snapshots = uiState.dailySnapshots,
                    trendDays = trendDays,
                    onTrendDaysChange = { trendDays = it }
                )
            }

            item {
                SectionTitle("持股摘要")
            }

            if (activeSummaries.isEmpty()) {
                item {
                    EmptyCard(
                        message = "目前還沒有任何持股資料",
                        containerColor = SectionHoldingColor
                    )
                }
            } else {
                itemsIndexed(
                    items = activeSummaries,
                    key = { index, summary ->
                        "holding_${summary.stockCode}_${summary.stockName.orEmpty()}_${summary.averageCostPerShare}_${summary.holdingShares}_$index"
                    }
                ) { index, summary ->
                    val expandKey = "${summary.stockCode}_$index"
                    val expanded = expandedSummaryMap[expandKey] ?: false

                    HoldingSummaryRowCard(
                        summary = summary,
                        expanded = expanded,
                        onToggle = {
                            expandedSummaryMap[expandKey] = !expanded
                        }
                    )
                }
            }

            item {
                SectionTitle("交易紀錄")
            }

            if (uiState.transactionGroups.isEmpty()) {
                item {
                    EmptyCard(
                        message = "尚未建立交易紀錄",
                        containerColor = SectionTransactionColor
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.transactionGroups,
                    key = { index, group ->
                        "tx_${group.stockCode}_${group.stockName}_${group.transactions.size}_$index"
                    }
                ) { _, group ->
                    TransactionGroupCard(
                        group = group,
                        onToggle = { viewModel.toggleTransactionGroup(group.stockCode) },
                        onDelete = { transaction -> viewModel.deleteTransaction(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    isRefreshing: Boolean,
    trackedCount: Int,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "追蹤中股票",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1565C0)
                )
                Text(
                    text = "$trackedCount 檔",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }

            Text(
                text = "可手動重新整理目前持股的最新報價",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("重新整理報價")
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioOverviewCard(
    summaries: List<PortfolioSummary>
) {
    val totalCost = summaries.sumOf { it.remainingCost }

    val totalMarketValue = summaries.sumOf { summary ->
        val price = summary.currentPrice ?: 0.0
        price * summary.holdingShares
    }

    val totalUnrealized = summaries.sumOf { it.unrealizedProfitNet ?: 0.0 }
    val totalRealized = summaries.sumOf { it.realizedProfit }
    val totalProfit = totalUnrealized + totalRealized

    val profitRate = if (totalCost == 0.0) {
        null
    } else {
        totalProfit / totalCost * 100.0
    }

    val totalProfitColor = when {
        totalProfit > 0 -> PositiveRed
        totalProfit < 0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    val unrealizedColor = when {
        totalUnrealized > 0 -> PositiveRed
        totalUnrealized < 0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    val realizedColor = when {
        totalRealized > 0 -> PositiveRed
        totalRealized < 0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SectionSummaryColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "投資組合摘要",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMiniChip(
                    modifier = Modifier.weight(1f),
                    label = "總成本",
                    value = formatCompactNumber(totalCost),
                    tone = MiniChipTone.COST
                )
                SummaryMiniChip(
                    modifier = Modifier.weight(1f),
                    label = "持股市值",
                    value = formatCompactNumber(totalMarketValue),
                    tone = MiniChipTone.PRICE
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMiniChipTwoLine(
                    modifier = Modifier.weight(1f),
                    label = "未實現損益",
                    primaryValue = formatCompactSignedMoney(totalUnrealized),
                    secondaryValue = formatIntMoney(totalUnrealized),
                    tone = MiniChipTone.PROFIT,
                    primaryColor = unrealizedColor,
                    secondaryColor = unrealizedColor.copy(alpha = 0.75f)
                )
                SummaryMiniChipTwoLine(
                    modifier = Modifier.weight(1f),
                    label = "已實現損益",
                    primaryValue = formatCompactSignedMoney(totalRealized),
                    secondaryValue = formatIntMoney(totalRealized),
                    tone = MiniChipTone.PROFIT,
                    primaryColor = realizedColor,
                    secondaryColor = realizedColor.copy(alpha = 0.75f)
                )
            }

            SummaryMiniChipTwoLine(
                modifier = Modifier.fillMaxWidth(),
                label = "總損益",
                primaryValue = formatCompactSignedMoney(totalProfit),
                secondaryValue = if (profitRate == null) {
                    formatIntMoney(totalProfit)
                } else {
                    "${formatIntMoney(totalProfit)} / ${percentFormat.format(profitRate)}%"
                },
                tone = MiniChipTone.PROFIT,
                primaryColor = totalProfitColor,
                secondaryColor = totalProfitColor.copy(alpha = 0.75f)
            )

            Text(
                text = "持股市值為未扣預估賣出手續費與交易稅。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionInputCard(
    stockCode: String,
    onStockCodeChange: (String) -> Unit,
    stockName: String?,
    suggestions: List<StockInfo>,
    onSelectSuggestion: (String) -> Unit,
    tradeDate: String,
    onTradeDateChange: (String) -> Unit,
    quantityInput: String,
    onQuantityChange: (String) -> Unit,
    quantityUnit: TradeQuantityUnit,
    onQuantityUnitChange: (TradeQuantityUnit) -> Unit,
    priceInput: String,
    onPriceChange: (String) -> Unit,
    noteInput: String,
    onNoteChange: (String) -> Unit,
    transactionType: TransactionType,
    onTransactionTypeChange: (TransactionType) -> Unit,
    isSubmitCoolingDown: Boolean,
    onSubmit: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var quantityMenuExpanded by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onTradeDateChange(formatMillisToDate(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SectionInputColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "新增交易",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = transactionType == TransactionType.BUY,
                    onClick = { onTransactionTypeChange(TransactionType.BUY) },
                    label = { Text("買進") }
                )
                FilterChip(
                    selected = transactionType == TransactionType.SELL,
                    onClick = { onTransactionTypeChange(TransactionType.SELL) },
                    label = { Text("賣出") }
                )
            }

            OutlinedTextField(
                value = stockCode,
                onValueChange = onStockCodeChange,
                label = { Text("股票代號，例如 2330") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (!stockName.isNullOrBlank()) {
                Text(
                    text = "股票名稱：$stockName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (suggestions.isNotEmpty()) {
                Text(
                    text = "快速選擇：",
                    style = MaterialTheme.typography.bodySmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.take(5).forEach { info ->
                        TextButton(
                            onClick = { onSelectSuggestion(info.stockCode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${info.stockCode} ${info.stockName}")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showDatePicker = true
                    }
            ) {
                OutlinedTextField(
                    value = tradeDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("交易日期") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = priceInput,
                onValueChange = onPriceChange,
                label = { Text("成交價格") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = onQuantityChange,
                    label = { Text("數量") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Box(modifier = Modifier.weight(0.8f)) {
                    OutlinedTextField(
                        value = quantityUnit.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("單位") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "選擇單位",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                quantityMenuExpanded = true
                            }
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = quantityMenuExpanded,
                        onDismissRequest = { quantityMenuExpanded = false }
                    ) {
                        TradeQuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label) },
                                onClick = {
                                    onQuantityUnitChange(unit)
                                    quantityMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = if (quantityUnit == TradeQuantityUnit.LOT) {
                    "目前以「張」輸入，例如 1 代表 1 張。"
                } else {
                    "目前以「股」輸入，例如 1000 代表 1000 股，儲存時會自動換算成 1 張。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = noteInput,
                onValueChange = onNoteChange,
                label = { Text("備註，可不填") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitCoolingDown
            ) {
                Text(if (isSubmitCoolingDown) "請稍候…" else "儲存交易")
            }
        }
    }
}


@Composable
private fun TotalSummaryCard(summaries: List<PortfolioSummary>) {
    val totalCost = summaries.sumOf { it.remainingCost }
    val totalValueNet = summaries.sumOf { summary ->
        val price = summary.currentPrice
        val value = if (price == null) 0.0 else price * summary.holdingShares
        val feeTax = summary.estimatedSellFeeAndTax ?: 0.0
        value - feeTax
    }
    val totalUnrealized = summaries.sumOf { it.unrealizedProfitNet ?: 0.0 }
    val totalRealized = summaries.sumOf { it.realizedProfit }
    val totalProfit = totalUnrealized + totalRealized
    val profitRate = if (totalCost == 0.0) null else totalProfit / totalCost * 100.0

    val profitColor = when {
        totalProfit > 0 -> PositiveRed
        totalProfit < 0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SectionSummaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "總資產統計（未實現含估算費稅）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            InfoRow("總成本", formatIntMoney(totalCost))
            InfoRow("持股市值（淨）", formatIntMoney(totalValueNet))
            InfoRow("未實現損益（淨）", formatSignedIntAmount(totalUnrealized), valueColor = profitColor)
            InfoRow("已實現損益", formatSignedIntAmount(totalRealized), valueColor = profitColor)
            InfoRow(
                "總損益",
                if (profitRate == null) formatSignedIntAmount(totalProfit)
                else "${formatSignedIntAmount(totalProfit)} (${percentFormat.format(profitRate)}%)",
                valueColor = profitColor
            )
        }
    }
}



@Composable
private fun HoldingSummaryRowCard(
    summary: PortfolioSummary,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val priceDiff = if (summary.currentPrice != null && summary.holdingShares > 0.0) {
        summary.currentPrice - summary.averageCostPerShare
    } else {
        null
    }

    val unrealizedColor = when {
        (summary.unrealizedProfitNet ?: 0.0) > 0 -> PositiveRed
        (summary.unrealizedProfitNet ?: 0.0) < 0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    val realizedColor = when {
        summary.realizedProfit > 0 -> PositiveRed
        summary.realizedProfit < 0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SectionHoldingColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (summary.stockName.isNullOrBlank()) {
                            summary.stockCode
                        } else {
                            "${summary.stockCode} ${summary.stockName}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "持有 ${formatHoldingDisplay(summary.holdingShares)}　｜　成本 ${formatIntMoney(summary.remainingCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收合 ${summary.stockCode}" else "展開 ${summary.stockCode}",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMiniChip(
                        modifier = Modifier.weight(1f),
                        label = "持有股數",
                        value = formatHoldingDisplay(summary.holdingShares),
                        tone = MiniChipTone.HOLDING
                    )
                    SummaryMiniChip(
                        modifier = Modifier.weight(1f),
                        label = "持有成本",
                        value = formatIntMoney(summary.remainingCost),
                        tone = MiniChipTone.COST
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMiniChip(
                        modifier = Modifier.weight(1f),
                        label = "平均成本",
                        value = formatPriceValue(summary.averageCostPerShare),
                        tone = MiniChipTone.COST
                    )
                    SummaryMiniChip(
                        modifier = Modifier.weight(1f),
                        label = "目前股價",
                        value = summary.currentPrice?.let { formatPriceValue(it) } ?: "尚未取得",
                        tone = MiniChipTone.PRICE
                    )
                    PriceDiffChip(
                        modifier = Modifier.weight(1f),
                        label = "價差",
                        value = priceDiff?.let { formatSignedPriceDiff(it) } ?: "—",
                        diff = priceDiff
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMiniChipTwoLine(
                        modifier = Modifier.weight(1f),
                        label = "未實現損益",
                        primaryValue = summary.unrealizedProfitNet?.let {
                            formatSignedIntAmount(it)
                        } ?: "需先更新",
                        secondaryValue = if (summary.unrealizedProfitNet == null) {
                            null
                        } else {
                            "${percentFormat.format(summary.unrealizedProfitRateNet ?: 0.0)}%"
                        },
                        tone = MiniChipTone.PROFIT,
                        primaryColor = unrealizedColor,
                        secondaryColor = unrealizedColor.copy(alpha = 0.75f)
                    )

                    SummaryMiniChipTwoLine(
                        modifier = Modifier.weight(1f),
                        label = "已實現損益",
                        primaryValue = formatSignedIntAmount(summary.realizedProfit),
                        secondaryValue = summary.realizedProfitRate?.let {
                            "${percentFormat.format(it)}%"
                        } ?: "—",
                        tone = MiniChipTone.PROFIT,
                        primaryColor = realizedColor,
                        secondaryColor = realizedColor.copy(alpha = 0.75f)
                    )
                }

                HorizontalDivider()

                val priceSource = detectPriceSource(
                    currentPrice = summary.currentPrice,
                    lastQuoteTime = summary.lastQuoteTime
                )

                PriceSourceChip(priceSource = priceSource)

                Text(
                    text = when (priceSource) {
                        PriceSourceLabel.INTRADAY -> "目前顯示盤中即時報價。"
                        PriceSourceLabel.CLOSING -> "目前顯示最近收盤價。"
                        PriceSourceLabel.CACHED -> "目前顯示最近一次成功更新的快取價格。"
                        PriceSourceLabel.UNKNOWN -> "目前尚未取得價格，請先更新現價。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "價格時間：${formatPriceUpdatedAt(summary.lastQuoteTime)}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (summary.estimatedSellFeeAndTax != null) {
                    Text(
                        text = "估算賣出費稅：${formatIntMoney(summary.estimatedSellFeeAndTax)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioTrendCard(
    snapshots: List<DailyPortfolioSnapshotEntity>,
    trendDays: Int,
    onTrendDaysChange: (Int) -> Unit
) {
    val defaultTrendColor = MaterialTheme.colorScheme.primary
    val neutralTextColor = MaterialTheme.colorScheme.onSurface
    val neutralSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trendOptions = listOf(5, 7, 10, 14, 21, 30)

    fun formatXAxisLabel(raw: String): String {
        return try {
            if (raw.length >= 10) raw.substring(5, 10) else raw
        } catch (_: Exception) {
            raw
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SectionSummaryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "投資組合走勢（最近 ${trendDays.coerceIn(1, 30)} 天）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Y 軸為總損益，X 軸為日期",
                style = MaterialTheme.typography.bodySmall,
                color = neutralSecondaryColor
            )

            val safeTrendDays = trendDays.coerceIn(1, 30)
            val points = snapshots.takeLast(safeTrendDays)

            if (points.size < 2) {
                Text(
                    text = "資料不足，至少需要 2 筆日快照才能顯示趨勢圖",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val values = points.map { it.totalProfit }
                val min = values.minOrNull() ?: 0.0
                val max = values.maxOrNull() ?: 0.0
                val latest = points.last()
                val latestValue = latest.totalProfit
                val average = values.average()
                val trendColor = when {
                    latestValue > 0 -> PositiveRed
                    latestValue < 0 -> NegativeGreen
                    else -> defaultTrendColor
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(top = 4.dp)
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height

                    val leftAxisWidth = 96f
                    val rightPadding = 20f
                    val topPadding = 20f
                    val bottomAxisHeight = 44f

                    val plotLeft = leftAxisWidth
                    val plotTop = topPadding
                    val plotRight = chartWidth - rightPadding
                    val plotBottom = chartHeight - bottomAxisHeight
                    val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
                    val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)

                    val adjustedMin = min.coerceAtMost(0.0).coerceAtMost(average)
                    val adjustedMax = max.coerceAtLeast(0.0).coerceAtLeast(average)
                    val range = (adjustedMax - adjustedMin).takeIf { it != 0.0 } ?: 1.0

                    fun xOf(index: Int): Float {
                        return plotLeft + index.toFloat() / (points.size - 1).coerceAtLeast(1) * plotWidth
                    }

                    fun yOf(value: Double): Float {
                        val ratio = ((value - adjustedMin) / range).toFloat()
                        return plotBottom - ratio * plotHeight
                    }

                    val tickCount = 5
                    val yTicks = (0 until tickCount).map { i ->
                        adjustedMax - (adjustedMax - adjustedMin) * i / (tickCount - 1)
                    }

                    yTicks.forEachIndexed { index, tickValue ->
                        val y = yOf(tickValue)
                        val isMiddleTick = index == tickCount / 2

                        drawLine(
                            color = if (isMiddleTick) {
                                neutralTextColor.copy(alpha = 0.28f)
                            } else {
                                Color.Gray.copy(alpha = 0.18f)
                            },
                            start = Offset(plotLeft, y),
                            end = Offset(plotRight, y),
                            strokeWidth = if (isMiddleTick) 2f else 1.2f
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            formatIntMoney(tickValue),
                            8f,
                            y + 10f,
                            android.graphics.Paint().apply {
                                color = neutralSecondaryColor.toArgb()
                                textSize = 28f
                                isAntiAlias = true
                            }
                        )
                    }

                    val averageY = yOf(average)
                    val dashWidth = 12f
                    val dashGap = 8f
                    var dashStart = plotLeft
                    while (dashStart < plotRight) {
                        val dashEnd = (dashStart + dashWidth).coerceAtMost(plotRight)
                        drawLine(
                            color = defaultTrendColor.copy(alpha = 0.6f),
                            start = Offset(dashStart, averageY),
                            end = Offset(dashEnd, averageY),
                            strokeWidth = 2f
                        )
                        dashStart += dashWidth + dashGap
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        "平均",
                        plotRight - 105f,
                        averageY - 8f,
                        android.graphics.Paint().apply {
                            color = defaultTrendColor.copy(alpha = 0.9f).toArgb()
                            textSize = 24f
                            isAntiAlias = true
                        }
                    )

                    drawLine(
                        color = neutralTextColor.copy(alpha = 0.55f),
                        start = Offset(plotLeft, plotTop),
                        end = Offset(plotLeft, plotBottom),
                        strokeWidth = 2f
                    )

                    drawLine(
                        color = neutralTextColor.copy(alpha = 0.55f),
                        start = Offset(plotLeft, plotBottom),
                        end = Offset(plotRight, plotBottom),
                        strokeWidth = 2f
                    )

                    val labelStep = when {
                        points.size <= 6 -> 1
                        points.size <= 12 -> 2
                        points.size <= 20 -> 3
                        else -> 5
                    }

                    points.forEachIndexed { index, point ->
                        val x = xOf(index)

                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(x, plotBottom),
                            end = Offset(x, plotBottom + 8f),
                            strokeWidth = 1.5f
                        )

                        val shouldDrawLabel =
                            index == 0 || index == points.lastIndex || index % labelStep == 0

                        if (shouldDrawLabel) {
                            drawContext.canvas.nativeCanvas.drawText(
                                formatXAxisLabel(point.snapshotDate),
                                x,
                                plotBottom + 28f,
                                android.graphics.Paint().apply {
                                    color = neutralSecondaryColor.toArgb()
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = xOf(index)
                        val y = yOf(point.totalProfit)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = trendColor,
                        style = Stroke(width = 4f)
                    )

                    points.forEachIndexed { index, point ->
                        val x = xOf(index)
                        val y = yOf(point.totalProfit)

                        drawCircle(
                            color = Color.White,
                            radius = 5.5f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = trendColor,
                            radius = 3.2f,
                            center = Offset(x, y)
                        )

                        val isLatest = index == points.lastIndex
                        val isPeak = point.totalProfit == max
                        val isBottom = point.totalProfit == min

                        if (isLatest || isPeak || isBottom) {
                            val highlightColor = when {
                                isLatest -> trendColor
                                point.totalProfit > 0 -> PositiveRed
                                point.totalProfit < 0 -> NegativeGreen
                                else -> neutralTextColor
                            }

                            drawCircle(
                                color = highlightColor,
                                radius = 6.5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMiniChipTwoLine(
                        modifier = Modifier.weight(1f),
                        label = "最新",
                        primaryValue = formatCompactSignedMoney(latestValue),
                        secondaryValue = formatIntMoney(latestValue),
                        tone = MiniChipTone.PROFIT,
                        primaryColor = trendColor,
                        secondaryColor = trendColor.copy(alpha = 0.75f)
                    )
                    SummaryMiniChipTwoLine(
                        modifier = Modifier.weight(1f),
                        label = "平均",
                        primaryValue = formatCompactSignedMoney(average),
                        secondaryValue = formatIntMoney(average),
                        tone = MiniChipTone.PROFIT,
                        primaryColor = defaultTrendColor,
                        secondaryColor = defaultTrendColor.copy(alpha = 0.75f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMiniChipTwoLine(
                        modifier = Modifier.weight(1f),
                        label = "區間最高",
                        primaryValue = formatCompactSignedMoney(max),
                        secondaryValue = formatIntMoney(max),
                        tone = MiniChipTone.PROFIT,
                        primaryColor = if (max > 0) PositiveRed else neutralTextColor,
                        secondaryColor = if (max > 0) {
                            PositiveRed.copy(alpha = 0.75f)
                        } else {
                            neutralSecondaryColor
                        }
                    )
                    SummaryMiniChipTwoLine(
                        modifier = Modifier.weight(1f),
                        label = "區間最低",
                        primaryValue = formatCompactSignedMoney(min),
                        secondaryValue = formatIntMoney(min),
                        tone = MiniChipTone.PROFIT,
                        primaryColor = if (min < 0) NegativeGreen else neutralTextColor,
                        secondaryColor = if (min < 0) {
                            NegativeGreen.copy(alpha = 0.75f)
                        } else {
                            neutralSecondaryColor
                        }
                    )
                }

                InfoRow("最新快照日期", latest.snapshotDate)
            }

            var trendMenuExpanded by remember { mutableStateOf(false) }

            Text(
                text = "顯示區間",
                style = MaterialTheme.typography.bodySmall,
                color = neutralSecondaryColor
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "最近 ${trendDays.coerceIn(1, 30)} 天",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("趨勢圖天數") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "展開天數選單",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            trendMenuExpanded = true
                        }
                )

                DropdownMenu(
                    expanded = trendMenuExpanded,
                    onDismissRequest = { trendMenuExpanded = false }
                ) {
                    trendOptions.forEach { days ->
                        DropdownMenuItem(
                            text = { Text("最近 ${days} 天") },
                            onClick = {
                                onTrendDaysChange(days)
                                trendMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceSourceChip(priceSource: PriceSourceLabel) {
    val (containerColor, labelColor) = when (priceSource) {
        PriceSourceLabel.INTRADAY -> Pair(Color(0xFFFFE0B2), Color(0xFF8D4E00))
        PriceSourceLabel.CLOSING -> Pair(Color(0xFFDCEBFF), Color(0xFF0D47A1))
        PriceSourceLabel.CACHED -> Pair(Color(0xFFEDE7F6), Color(0xFF5E35B1))
        PriceSourceLabel.UNKNOWN -> Pair(Color(0xFFE0E0E0), Color(0xFF616161))
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = priceSource.label,
                color = labelColor,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor
        )
    )
}

@Composable
private fun TransactionGroupCard(
    group: TransactionGroupUi,
    onToggle: () -> Unit,
    onDelete: (StockTransaction) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SectionTransactionColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (group.stockName.isBlank()) {
                            group.stockCode
                        } else {
                            "${group.stockCode} ${group.stockName}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "共 ${group.transactions.size} 筆交易，點擊展開查看",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (group.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (group.isExpanded) "收合 ${group.stockCode}" else "展開 ${group.stockCode}",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (group.isExpanded) {
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.transactionRecords.forEach { record ->
                        TransactionRow(
                            record = record,
                            onDelete = { onDelete(record.transaction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    record: TransactionRecordUi,
    onDelete: () -> Unit
) {
    val transaction = record.transaction
    val typeColor = if (transaction.transactionType == TransactionType.BUY) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFAD1457)
    }
    val netAmountLabel = when (transaction.transactionType) {
        TransactionType.BUY -> "買進實付"
        TransactionType.SELL -> "賣出實收"
    }
    val realizedProfitColor = when {
        record.realizedProfit == null -> MaterialTheme.colorScheme.onSurface
        record.realizedProfit > 0.0 -> PositiveRed
        record.realizedProfit < 0.0 -> NegativeGreen
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (transaction.transactionType == TransactionType.BUY) "買進" else "賣出",
                    color = typeColor,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(onClick = onDelete) {
                    Text("刪除")
                }
            }

            InfoRow("日期", transaction.tradeDate)
            InfoRow("價格", formatPriceValue(transaction.pricePerShare))
            InfoRow("張數", lotFormat.format(transaction.lotCount))
            InfoRow("股數", formatIntValue(transaction.shareCount.toDouble()))
            InfoRow("成交金額", formatIntMoney(transaction.grossAmount))
            InfoRow("手續費", formatIntMoney(record.brokerageFee))

            if (transaction.transactionType == TransactionType.SELL) {
                InfoRow("證交稅", formatIntMoney(record.tax))
                record.costBasis?.let { costBasis ->
                    InfoRow("賣出成本", formatIntMoney(costBasis))
                }
            }

            InfoRow(netAmountLabel, formatIntMoney(record.netAmount), valueColor = typeColor)

            if (transaction.transactionType == TransactionType.SELL) {
                val profitText = record.realizedProfit?.let { profit ->
                    val rateText = record.realizedProfitRate?.let { "（${percentFormat.format(it)}%）" }.orEmpty()
                    "${formatSignedIntAmount(profit)}$rateText"
                } ?: "無可用持股成本"

                InfoRow("賣出損益", profitText, valueColor = realizedProfitColor)
            }

            if (transaction.note.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SummaryMiniChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tone: MiniChipTone = MiniChipTone.NEUTRAL,
    valueColor: Color? = null
) {
    val resolvedValueColor = valueColor ?: MaterialTheme.colorScheme.onSurface

    val containerColor = when (tone) {
        MiniChipTone.HOLDING -> Color(0xFFF4F7FF)
        MiniChipTone.COST -> Color(0xFFFFF6EC)
        MiniChipTone.PRICE -> Color(0xFFF3FBF6)
        MiniChipTone.PROFIT -> Color(0xFFFDF2F4)
        MiniChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val labelColor = when (tone) {
        MiniChipTone.HOLDING -> Color(0xFF4A5A8A)
        MiniChipTone.COST -> Color(0xFF8A5A2B)
        MiniChipTone.PRICE -> Color(0xFF2D6A4F)
        MiniChipTone.PROFIT -> Color(0xFF8A3A56)
        MiniChipTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = resolvedValueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryMiniChipTwoLine(
    modifier: Modifier = Modifier,
    label: String,
    primaryValue: String,
    secondaryValue: String? = null,
    tone: MiniChipTone = MiniChipTone.NEUTRAL,
    primaryColor: Color? = null,
    secondaryColor: Color? = null
) {
    val resolvedPrimaryColor = primaryColor ?: MaterialTheme.colorScheme.onSurface
    val resolvedSecondaryColor = secondaryColor ?: MaterialTheme.colorScheme.onSurfaceVariant

    val containerColor = when (tone) {
        MiniChipTone.HOLDING -> Color(0xFFF4F7FF)
        MiniChipTone.COST -> Color(0xFFFFF6EC)
        MiniChipTone.PRICE -> Color(0xFFF3FBF6)
        MiniChipTone.PROFIT -> Color(0xFFFDF2F4)
        MiniChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val labelColor = when (tone) {
        MiniChipTone.HOLDING -> Color(0xFF4A5A8A)
        MiniChipTone.COST -> Color(0xFF8A5A2B)
        MiniChipTone.PRICE -> Color(0xFF2D6A4F)
        MiniChipTone.PROFIT -> Color(0xFF8A3A56)
        MiniChipTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = primaryValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = resolvedPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!secondaryValue.isNullOrBlank()) {
                    Text(
                        text = secondaryValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = resolvedSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceDiffChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    diff: Double?
) {
    val containerColor = when {
        diff == null -> NeutralGrayBg
        diff > 0 -> PositiveRedBg
        diff < 0 -> NegativeGreenBg
        else -> NeutralGrayBg
    }

    val textColor = when {
        diff == null -> NeutralGrayText
        diff > 0 -> WhiteText
        diff < 0 -> WhiteText
        else -> NeutralGrayText
    }

    Card(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = if (textColor == WhiteText) 0.92f else 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    val resolvedColor = valueColor ?: MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = resolvedColor
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyCard(
    message: String,
    containerColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp)
        )
    }
}
