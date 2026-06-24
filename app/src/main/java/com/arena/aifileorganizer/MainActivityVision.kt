package com.arena.aifileorganizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arena.aifileorganizer.data.ApiKeyStore
import com.arena.aifileorganizer.ui.screens.HomeScreenVision
import com.arena.aifileorganizer.ui.screens.ResultScreenVision
import com.arena.aifileorganizer.ui.screens.ScanScreenVision
import com.arena.aifileorganizer.ui.theme.AIFileOrganizerThemeVision

/**
 * MainActivity – visionOS Liquid Glass v1.1
 * To enable: change AndroidManifest android:name=".MainActivity"
 * to ".MainActivityVision" or rename this file to MainActivity.kt
 */
class MainActivityVision : ComponentActivity() {

    private lateinit var organizerViewModel: OrganizerViewModel
    private lateinit var apiKeyStore: ApiKeyStore

    private val openTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            organizerViewModel.setTreeUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKeyStore = ApiKeyStore(this)
        organizerViewModel = ViewModelProvider(this, object: ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OrganizerViewModel(application, apiKeyStore) as T
            }
        })[OrganizerViewModel::class.java]

        setContent {
            AIFileOrganizerThemeVision(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavVision(
                        vm = organizerViewModel,
                        apiKeyStore = apiKeyStore,
                        onPickFolder = { openTreeLauncher.launch(null) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavVision(
    vm: OrganizerViewModel,
    apiKeyStore: ApiKeyStore,
    onPickFolder: () -> Unit
) {
    val nav = rememberNavController()
    val apiKey by apiKeyStore.apiKeyFlow.collectAsState(initial = apiKeyStore.getApiKey())

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreenVision(
                apiKey = apiKey,
                treeUri = vm.treeUri.value,
                onSaveApiKey = { apiKeyStore.saveApiKey(it) },
                onPickFolder = onPickFolder,
                onStartScan = { nav.navigate("scan") },
                canStart = apiKeyStore.hasKey() && vm.treeUri.value != null
            )
        }
        composable("scan") {
            ScanScreenVision(
                vm = vm,
                onDone = { nav.navigate("result") { popUpTo("home") } },
                onBack = { nav.popBackStack() }
            )
        }
        composable("result") {
            ResultScreenVision(
                vm = vm,
                onBackHome = { nav.popBackStack("home", inclusive = false) }
            )
        }
    }
}
