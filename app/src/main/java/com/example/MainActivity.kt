package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.screens.BatchSigningScreen
import com.example.ui.screens.DocDetailDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignPdfScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppTab(val label: String) {
    SIGN("Tunggal"),
    BATCH("Batch Auto"),
    HISTORY("Riwayat"),
    SETTINGS("Pengaturan")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val paletteStyle by viewModel.paletteStyle.collectAsState()

            MyApplicationTheme(
                themeMode = themeMode,
                paletteStyle = paletteStyle
            ) {
                var selectedTab by remember { mutableStateOf(AppTab.SIGN) }
                val detailItem by viewModel.detailItem.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.QrCodeScanner,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "PDF QR Signer",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Auto-Detection & Verification Engine",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
                                        contentDescription = "Tunggal"
                                    )
                                },
                                label = { Text(AppTab.SIGN.label) },
                                modifier = Modifier.testTag("tab_sign")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.BATCH,
                                onClick = { selectedTab = AppTab.BATCH },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == AppTab.BATCH) Icons.Filled.DynamicFeed else Icons.Outlined.DynamicFeed,
                                        contentDescription = "Batch Auto"
                                    )
                                },
                                label = { Text(AppTab.BATCH.label) },
                                modifier = Modifier.testTag("tab_batch")
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
                                selected = selectedTab == AppTab.SETTINGS,
                                onClick = { selectedTab = AppTab.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Pengaturan"
                                    )
                                },
                                label = { Text(AppTab.SETTINGS.label) },
                                modifier = Modifier.testTag("tab_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        AppTab.SIGN -> SignPdfScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        AppTab.BATCH -> BatchSigningScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        AppTab.HISTORY -> HistoryScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
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
