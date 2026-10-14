package com.liulkovich.florapoint.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.liulkovich.florapoint.presentation.auth.AuthManager
import com.liulkovich.florapoint.presentation.navigation.NavGraph
import com.liulkovich.florapoint.presentation.screens.home.HomeViewModel
import com.liulkovich.florapoint.presentation.ui.theme.FloraPointTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val updateSnackbarHostState = SnackbarHostState()
    val homeViewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var authManager: AuthManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.d("UPDATE", "Update flow failed or cancelled: ${result.resultCode}")
        }
    }

    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestartSnackbar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        requestNotificationPermission()
        appUpdateManager.registerListener(installStateUpdatedListener)
        checkForUpdate()


        splashScreen.setKeepOnScreenCondition {
            val state = homeViewModel.state.value
            state.isLoading || state.isTipLoading
        }

        authManager.signInAnonymously(
            onSuccess = {
                Log.d("AUTH", "UID = ${authManager.getCurrentUserId()}")
                if (authManager.isRegistered()) {
                    homeViewModel.syncPointsFromCloud()
                }
            },
            onError = {
                Log.d("AUTH", "ERROR = $it")
            }
        )

        setContent {
            FloraPointTheme {
                NavGraph(snackbarHostState = updateSnackbarHostState)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar()
            }
        }.addOnFailureListener {
            Log.d("UPDATE", "Update check failed: $it")
        }
    }

    private fun showRestartSnackbar() {
        lifecycleScope.launch {
            val result = updateSnackbarHostState.showSnackbar(
                message = getString(
                    com.liulkovich.florapoint.R.string.update_ready
                ),
                actionLabel = getString(
                    com.liulkovich.florapoint.R.string.restart
                ),
                duration = SnackbarDuration.Indefinite
            )

            if (result == SnackbarResult.ActionPerformed) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}