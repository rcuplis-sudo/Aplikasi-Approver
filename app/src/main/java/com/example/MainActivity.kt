package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.MainViewModel
import com.example.ui.screens.AlgorithmInfoScreen
import com.example.ui.screens.DocDetailDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SignPdfScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppTab(val label: String) {
    SIGN("Tanda Tangani"),
    HISTORY("Riwayat Room"),
    INFO("Info & Alur")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableStateOf(AppTab.SIGN) }
                val detailItem by viewModel.detailItem.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "PDF QR Signer",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.testTag("app_bottom_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == AppTab.SIGN,
                                onClick = { selectedTab = AppTab.SIGN },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == AppTab.SIGN) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                                        contentDescription = "Tanda Tangani"
                                    )
                                },
                                label = { Text(AppTab.SIGN.label) },
                                modifier = Modifier.testTag("tab_sign")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.HISTORY,
                                onClick = { selectedTab = AppTab.HISTORY },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == AppTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "Riwayat"
                                    )
                                },
                                label = { Text(AppTab.HISTORY.label) },
                                modifier = Modifier.testTag("tab_history")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.INFO,
                                onClick = { selectedTab = AppTab.INFO },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == AppTab.INFO) Icons.Filled.Info else Icons.Outlined.Info,
                                        contentDescription = "Info Alur"
                                    )
                                },
                                label = { Text(AppTab.INFO.label) },
                                modifier = Modifier.testTag("tab_info")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        AppTab.SIGN -> SignPdfScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        AppTab.HISTORY -> HistoryScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        AppTab.INFO -> AlgorithmInfoScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    // Inspection dialog when item selected from history
                    if (detailItem != null) {
                        DocDetailDialog(
                            document = detailItem!!,
                            onDismiss = { viewModel.dismissDetail() }
                        )
                    }
                }
            }
        }
    }
}
