
package com.liulkovich.florapoint.shared.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedSettingsLanguage
import com.liulkovich.florapoint.shared.domain.SharedSettingsState
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedSettingsScreen(
    state: SharedSettingsState,
    onGoogleSignIn: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNotifications: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onOfflineMaps: () -> Unit,
    onPrivacy: () -> Unit,
    onLanguageSelected: (SharedSettingsLanguage) -> Unit,
    onContactDeveloper: () -> Unit,
    onRateApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageMenu by remember {
        mutableStateOf(false)
    }

    var showBackupDialog by remember {
        mutableStateOf(false)
    }

    var showRestoreDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(
                Res.string.settings_screen_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(
                top = 8.dp,
                bottom = 12.dp
            )
        )

        SharedSettingsSection(
            title = stringResource(
                Res.string.settings_account
            )
        ) {
            if (state.isAuthorized) {
                SharedSettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = state.userName
                        ?: stringResource(
                            Res.string.settings_default_user
                        ),
                    subtitle = state.userEmail.orEmpty(),
                    showArrow = false
                )

                SettingsDivider()

                SharedSettingsItem(
                    icon = Icons.Default.Logout,
                    title = stringResource(
                        Res.string.settings_logout
                    ),
                    subtitle = stringResource(
                        Res.string.settings_logout_description
                    ),
                    onClick = onLogout
                )

                SettingsDivider()

                SharedSettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(
                        Res.string.settings_delete_account
                    ),
                    subtitle = stringResource(
                        Res.string.settings_delete_account_description
                    ),
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = {
                        showDeleteDialog = true
                    }
                )
            } else {
                SharedSettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = stringResource(
                        Res.string.settings_sign_in_google
                    ),
                    subtitle = stringResource(
                        Res.string.settings_sign_in_description
                    ),
                    onClick = onGoogleSignIn
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SharedSettingsSection(
            title = stringResource(
                Res.string.settings_notifications
            )
        ) {
            SharedSettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(
                    Res.string.settings_seasonal
                ),
                subtitle = stringResource(
                    Res.string.settings_seasonal_description
                ),
                onClick = onNotifications
            )
        }

        Spacer(Modifier.height(12.dp))

        SharedSettingsSection(
            title = stringResource(
                Res.string.settings_my_data
            )
        ) {
            SharedSettingsItem(
                icon = Icons.Default.Backup,
                title = stringResource(
                    Res.string.settings_backup
                ),
                subtitle = stringResource(
                    Res.string.settings_backup_description
                ),
                onClick = {
                    showBackupDialog = true
                }
            )

            SettingsDivider()

            SharedSettingsItem(
                icon = Icons.Default.Restore,
                title = stringResource(
                    Res.string.settings_restore
                ),
                subtitle = stringResource(
                    Res.string.settings_restore_description
                ),
                onClick = {
                    showRestoreDialog = true
                }
            )

            SettingsDivider()

            SharedSettingsItem(
                icon = Icons.Default.Download,
                title = stringResource(
                    Res.string.settings_offline_maps
                ),
                subtitle = stringResource(
                    Res.string.settings_offline_description
                ),
                onClick = onOfflineMaps
            )
        }

        Spacer(Modifier.height(12.dp))

        SharedSettingsSection(
            title = stringResource(
                Res.string.settings_about
            )
        ) {
            SharedSettingsItem(
                icon = Icons.Default.Lock,
                title = stringResource(
                    Res.string.settings_privacy
                ),
                subtitle = stringResource(
                    Res.string.settings_privacy_description
                ),
                onClick = onPrivacy
            )

            SettingsDivider()

            Box {
                val languageName = when (state.language) {
                    SharedSettingsLanguage.SYSTEM ->
                        stringResource(Res.string.settings_system)

                    SharedSettingsLanguage.RUSSIAN ->
                        stringResource(Res.string.settings_russian)

                    SharedSettingsLanguage.ENGLISH ->
                        stringResource(Res.string.settings_english)
                }

                SharedSettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(
                        Res.string.settings_language
                    ),
                    subtitle = languageName,
                    onClick = {
                        showLanguageMenu = true
                    }
                )

                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = {
                        showLanguageMenu = false
                    }
                ) {
                    val languages = listOf(
                        SharedSettingsLanguage.SYSTEM to
                                Res.string.settings_system,
                        SharedSettingsLanguage.RUSSIAN to
                                Res.string.settings_russian,
                        SharedSettingsLanguage.ENGLISH to
                                Res.string.settings_english
                    )

                    languages.forEach { (language, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(label))
                            },
                            onClick = {
                                showLanguageMenu = false
                                onLanguageSelected(language)
                            },
                            leadingIcon = {
                                if (state.language == language) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF66BB6A)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            SettingsDivider()

            SharedSettingsItem(
                icon = Icons.Default.Mail,
                title = stringResource(
                    Res.string.settings_contact
                ),
                subtitle = stringResource(
                    Res.string.settings_contact_description
                ),
                onClick = onContactDeveloper
            )

            SettingsDivider()

            SharedSettingsItem(
                icon = Icons.Default.Star,
                title = stringResource(
                    Res.string.settings_rate
                ),
                subtitle = stringResource(
                    Res.string.settings_rate_description
                ),
                onClick = onRateApp
            )

            SettingsDivider()

            SharedSettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(
                    Res.string.settings_version
                ),
                subtitle = state.appVersion,
                showArrow = false
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showBackupDialog) {
        SharedSettingsConfirmationDialog(
            title = stringResource(Res.string.settings_backup),
            message = stringResource(
                Res.string.settings_backup_warning
            ),
            confirmText = stringResource(
                Res.string.settings_save
            ),
            onDismiss = {
                showBackupDialog = false
            },
            onConfirm = {
                showBackupDialog = false
                onBackup()
            }
        )
    }

    if (showRestoreDialog) {
        SharedSettingsConfirmationDialog(
            title = stringResource(Res.string.settings_restore),
            message = stringResource(
                Res.string.settings_restore_warning
            ),
            confirmText = stringResource(
                Res.string.settings_choose_file
            ),
            onDismiss = {
                showRestoreDialog = false
            },
            onConfirm = {
                showRestoreDialog = false
                onRestore()
            }
        )
    }

    if (showDeleteDialog) {
        SharedSettingsConfirmationDialog(
            title = stringResource(
                Res.string.settings_delete_account
            ),
            message = stringResource(
                Res.string.settings_delete_warning
            ),
            confirmText = stringResource(
                Res.string.settings_delete
            ),
            destructive = true,
            onDismiss = {
                showDeleteDialog = false
            },
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccount()
            }
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun SharedSettingsConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFF66BB6A)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        Res.string.settings_cancel
                    )
                )
            }
        }
    )
}
