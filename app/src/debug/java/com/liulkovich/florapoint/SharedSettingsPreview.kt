
package com.liulkovich.florapoint

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.liulkovich.florapoint.presentation.screens.settings.SettingsViewModel
import com.liulkovich.florapoint.shared.domain.SharedSettingsLanguage
import com.liulkovich.florapoint.shared.domain.SharedSettingsState
import com.liulkovich.florapoint.shared.ui.settings.SharedSettingsScreen

@Composable
fun SharedSettingsPreview(
    onNavigateToNotifications: () -> Unit,
    onNavigateToOfflineRegions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val selectedLanguage = when (currentLanguage) {
        "ru" -> SharedSettingsLanguage.RUSSIAN
        "en" -> SharedSettingsLanguage.ENGLISH
        else -> SharedSettingsLanguage.SYSTEM
    }

    val state = SharedSettingsState(
        isAuthorized = authState.isAuthorized,
        userName = authState.userName,
        userEmail = authState.userEmail,
        language = selectedLanguage,
        appVersion = BuildConfig.VERSION_NAME
    )

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.handleImport(context, it)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(
            result.data,
            onSuccess = {
                Toast.makeText(
                    context,
                    R.string.sign_in_success,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { error ->
                Log.e("SharedSettings", "Sign-in failed: $error")

                Toast.makeText(
                    context,
                    context.getString(
                        R.string.sign_in_error,
                        error
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    SharedSettingsScreen(
        modifier = modifier,
        state = state,

        onGoogleSignIn = {
            val intent = viewModel.getGoogleSignInIntent(context)
            googleSignInLauncher.launch(intent)
        },

        onLogout = {
            viewModel.logout()
        },

        onDeleteAccount = {
            viewModel.deleteAccount(
                onSuccess = {
                    Toast.makeText(
                        context,
                        R.string.account_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onError = { error ->
                    Toast.makeText(
                        context,
                        error.ifBlank {
                            context.getString(
                                R.string.delete_account_error
                            )
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        },

        onNotifications = onNavigateToNotifications,

        onBackup = {
            viewModel.exportPoints(context) { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/gpx+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.save_backup_title)
                )

                runCatching {
                    context.startActivity(chooser)
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Export failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        },

        onRestore = {
            importLauncher.launch("*/*")
        },

        onOfflineMaps = onNavigateToOfflineRegions,

        onPrivacy = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://rpuH41.github.io/forestpoint-privacy"
                )
            )

            runCatching {
                context.startActivity(intent)
            }
        },

        onLanguageSelected = { language ->
            val languageTag = when (language) {
                SharedSettingsLanguage.SYSTEM -> ""
                SharedSettingsLanguage.RUSSIAN -> "ru"
                SharedSettingsLanguage.ENGLISH -> "en"
            }

            viewModel.setLanguage(languageTag)
        },

        onContactDeveloper = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf("forestpoint.app@gmail.com")
                )
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    context.getString(R.string.feedback_subject)
                )
            }

            runCatching {
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.getString(R.string.write_letter)
                    )
                )
            }
        },

        onRateApp = {
            val packageName = context.packageName

            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            )

            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://play.google.com/store/apps/details?id=$packageName"
                )
            )

            runCatching {
                context.startActivity(marketIntent)
            }.onFailure {
                runCatching {
                    context.startActivity(browserIntent)
                }
            }
        }
    )
}
