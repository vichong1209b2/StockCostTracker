package com.example.stockcosttracker.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockcosttracker.domain.model.TransactionType
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: PortfolioViewModel = viewModel(
        factory = PortfolioViewModel.provideFactory(application)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val stockInfoMap = remember(uiState.stockInfos) {
        uiState.stockInfos.associateBy { it.stockCode }
    }

    var stockCode by rememberSaveable { mutableStateOf("") }
    var tradeDate by rememberSaveable { mutableStateOf(todayString()) }
    var quantityInput by rememberSaveable { mutableStateOf("") }
    var quantityUnit by rememberSaveable { mutableStateOf(TradeQuantityUnit.LOT) }
    var priceInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var transactionType by rememberSaveable { mutableStateOf(TransactionType.BUY) }

    var isSubmitCoolingDown by rememberSaveable { mutableStateOf(false) }
    var showSubmitSuccessDialog by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshStockInfos()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showSubmitSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitSuccessDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("返回首頁")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSubmitSuccessDialog = false
                        stockCode = ""
                        tradeDate = todayString()
                        quantityInput = ""
                        quantityUnit = TradeQuantityUnit.LOT
                        priceInput = ""
                        noteInput = ""
                        transactionType = TransactionType.BUY
                        isSubmitCoolingDown = false
                    }
                ) {
                    Text("繼續新增")
                }
            },
            title = {
                Text("新增成功")
            },
            text = {
                Text("交易已新增完成。")
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增交易") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TransactionInputCard(
                stockCode = stockCode,
                onStockCodeChange = { input ->
                    stockCode = input.uppercase(Locale.getDefault())
                },
                stockName = stockInfoMap[stockCode.trim()]?.stockName,
                suggestions = buildSuggestions(stockCode, uiState.stockInfos),
                onSelectSuggestion = { selectedCode ->
                    stockCode = selectedCode
                },
                tradeDate = tradeDate,
                onTradeDateChange = { tradeDate = it },
                quantityInput = quantityInput,
                onQuantityChange = { quantityInput = it },
                quantityUnit = quantityUnit,
                onQuantityUnitChange = { quantityUnit = it },
                priceInput = priceInput,
                onPriceChange = { priceInput = it },
                noteInput = noteInput,
                onNoteChange = { noteInput = it },
                transactionType = transactionType,
                onTransactionTypeChange = { transactionType = it },
                isSubmitCoolingDown = isSubmitCoolingDown,
                onSubmit = {
                    if (isSubmitCoolingDown) return@TransactionInputCard

                    val lotInput = quantityToLotInput(
                        quantityInput = quantityInput,
                        quantityUnit = quantityUnit
                    )

                    viewModel.addTransaction(
                        stockCode = stockCode,
                        tradeDate = tradeDate,
                        transactionType = transactionType,
                        priceInput = priceInput,
                        lotInput = lotInput,
                        note = noteInput
                    )

                    isSubmitCoolingDown = true
                    showSubmitSuccessDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}