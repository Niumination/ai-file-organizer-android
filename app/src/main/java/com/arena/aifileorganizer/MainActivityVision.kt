package com.arena.aifileorganizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.arena.aifileorganizer.data.SettingsStore
import com.arena.aifileorganizer.ui.screens.HomeScreenVision
import com.arena.aifileorganizer.ui.screens.ResultScreenVision
import com.arena.aifileorganizer.ui.screens.ScanScreenVision
import com.arena.aifileorganizer.ui.theme.AIFileOrganizerThemeVision

/**
 * MainActivity – visionOS Liquid Glass.
 *
 * - Edge-to-edge (transparent system bars handled by the theme).
 * - Persists the picked SAF folder across restarts; the persisted URI is
 *   validated against actually-granted permissions on startup.
 */
class MainActivityVision : ComponentActivity() {

    private lateinit var organizerViewModel: OrganizerViewModel
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var settingsStore: SettingsStore

    private val openTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
            settingsStore.treeUri = uri.toString()
            organizerViewModel.setTreeUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        apiKeyStore = ApiKeyStore(this)
        settingsStore = SettingsStore(this)
        organizerViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OrganizerViewModel(application, apiKeyStore) as T
            }
        })[OrganizerViewModel::class.java]

        // Restore the previously picked folder if its SAF grant is still valid.
        restorePersistedTreeUri()?.let { organizerViewModel.setTreeUri(it) }

        setContent {
            AIFileOrganizerThemeVision(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavVision(
                        vm = organizerViewModel,
                        apiKeyStore = apiKeyStore,
                        onPickFolder = { runCatching { openTreeLauncher.launch(null) } },
                        onClearFolder = { releasePersistedTreeUri() }
                    )
                }
            }
        }
    }

    private fun restorePersistedTreeUri(): Uri? {
        val saved = settingsStore.treeUri ?: return null
        val uri = runCatching { Uri.parse(saved) }.getOrNull() ?: return null
        val stillGranted = contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        return if (stillGranted) uri else {
            settingsStore.treeUri = null
            null
        }
    }

    private fun releasePersistedTreeUri() {
        settingsStore.treeUri?.let { saved ->
            runCatching { Uri.parse(saved) }.getOrNull()?.let { uri ->
                runCatching {
                    contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
        }
        settingsStore.treeUri = null
        organizerViewModel.setTreeUri(null)
        organizerViewModel.cancelScan()
    }
}

/** Friendly display label for a SAF tree uri, e.g. "Penyimpanan internal/Download". */
private fun friendlyFolderLabel(uri: Uri?): String? {
    uri ?: return null
    val raw = uri.lastPathSegment?.let { Uri.decode(it) } ?: return "Folder terpilih"
    return when {
        raw.startsWith("primary:") -> "Penyimpanan internal/" + raw.removePrefix("primary:")
        raw.startsWith("home:") -> "Penyimpanan internal/Dokumen/" + raw.removePrefix("home:")
        raw.contains(":") -> {
            val (volume, path) = raw.split(":", limit = 2).let {
                it[0] to (it.getOrNull(1) ?: "")
            }
            "SD Card ($volume)/$path"
        }
        else -> raw
    }.trimEnd('/')
}

@Composable
fun AppNavVision(
    vm: OrganizerViewModel,
    apiKeyStore: ApiKeyStore,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit
) {
    val nav = rememberNavController()
    val apiKey by apiKeyStore.apiKeyFlow.collectAsState()
    val treeUri = vm.treeUri.value

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreenVision(
                apiKey = apiKey,
                treeUri = treeUri,
                folderLabel = friendlyFolderLabel(treeUri),
                onSaveApiKey = { apiKeyStore.saveApiKey(it) },
                onClearApiKey = { apiKeyStore.clear() },
                onPickFolder = onPickFolder,
                onClearFolder = onClearFolder,
                onStartScan = { nav.navigate("scan") },
                canStart = !apiKey.isNullOrBlank() && treeUri != null
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
