package com.therapytrack.android.ui.client

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.therapytrack.android.BuildConfig
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ConsentState
import com.therapytrack.android.core.ErasurePreview
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(state: ClientState, onPrivacy: () -> Unit, onCrisis: () -> Unit, onSignedOut: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    val p = state.patient
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        com.therapytrack.android.ui.common.ProfileHeader(p?.name ?: "", stringResource(R.string.role_patient), p?.email ?: "", stringResource(R.string.role_patient))
        com.therapytrack.android.ui.common.Overline(stringResource(R.string.personal_info), modifier = Modifier.padding(top = 12.dp))
        com.therapytrack.android.ui.common.ListCard {
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.name), trailing = p?.name ?: "")
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.email), trailing = p?.email ?: "", divider = false)
        }
        com.therapytrack.android.ui.common.Overline(stringResource(R.string.your_care), modifier = Modifier.padding(top = 12.dp))
        com.therapytrack.android.ui.common.ListCard {
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.your_therapist), trailing = p?.therapistName ?: "—")
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.diagnosis_label), trailing = p?.diagnosis?.takeIf { it.isNotBlank() && it != "—" } ?: "—", divider = false)
        }
        com.therapytrack.android.ui.common.Overline(stringResource(R.string.app_settings), modifier = Modifier.padding(top = 12.dp))
        com.therapytrack.android.ui.common.ListCard {
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.language_setting), trailing = java.util.Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercase() }, icon = androidx.compose.material.icons.Icons.Outlined.Language)
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.crisis_support), icon = androidx.compose.material.icons.Icons.Outlined.Emergency, tint = TherapyColors.critical, chevron = true, divider = false, onClick = onCrisis)
        }
        com.therapytrack.android.ui.common.Overline(stringResource(R.string.data_privacy), modifier = Modifier.padding(top = 12.dp))
        com.therapytrack.android.ui.common.ListCard {
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.your_data), subtitle = stringResource(R.string.your_data_detail), icon = androidx.compose.material.icons.Icons.Outlined.Shield, chevron = true, onClick = onPrivacy)
            com.therapytrack.android.ui.common.ListRow(stringResource(R.string.sign_out), icon = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Logout, tint = TherapyColors.critical, titleColor = TherapyColors.critical, divider = false) { confirm = true }
        }
        com.therapytrack.android.ui.common.Overline(stringResource(R.string.app_info), modifier = Modifier.padding(top = 12.dp))
        com.therapytrack.android.ui.common.ListCard { com.therapytrack.android.ui.common.ListRow(stringResource(R.string.version_label), trailing = BuildConfig.VERSION_NAME, divider = false) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.sign_out)) }, text = { Text(stringResource(R.string.sign_out_confirm)) },
        confirmButton = { TextButton({ confirm = false; scope.launch { container.api.signOut(); onSignedOut() } }) { Text(stringResource(R.string.sign_out), color = TherapyColors.critical) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } })
}

/** Consent state, the Art. 15 export, and the Art. 17 request — with the preview shown before the act. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, onErased: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var consent by remember { mutableStateOf<ConsentState?>(null) }
    var preview by remember { mutableStateOf<ErasurePreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var erased by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { consent = container.api.consent() }
        runCatching { preview = container.api.erasurePreview().whatWillHappen }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.your_data), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)

        SectionTitle(stringResource(R.string.consents_title))
        listOf("clinical_data" to R.string.consent_clinical, "ai_drafting" to R.string.consent_ai).forEach { (purpose, label) ->
            val entry = consent?.consent?.get(purpose)
            Card {
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    when {
                        entry == null || !entry.granted -> Pill(stringResource(R.string.not_granted), TherapyColors.muted)
                        !entry.currentDocument -> Pill(stringResource(R.string.outdated_consent), TherapyColors.warning)
                        else -> Pill(stringResource(R.string.granted), TherapyColors.success)
                    }
                }
                consent?.purposes?.get(purpose)?.let { Muted(it, Modifier.padding(top = 4.dp)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val grant = entry?.granted != true || entry.currentDocument.not()
                    TextButton({ scope.launch { runCatching { container.api.recordConsent(purpose, grant); consent = container.api.consent() }.onFailure { error = it.message } } }) {
                        Text(stringResource(if (grant) R.string.granted else R.string.not_granted))
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.export_data))
        Card {
            Muted(stringResource(R.string.export_detail))
            Spacer(Modifier.height(10.dp))
            PrimaryButton(stringResource(R.string.export_data), loading = busy) {
                busy = true
                scope.launch {
                    try {
                        val json = container.api.exportMyData()
                        // Handed to the share sheet from app-private storage; nothing lands in Downloads unasked.
                        val dir = File(context.cacheDir, "export").apply { mkdirs() }
                        val file = File(dir, "therapytrack-export.json").apply { writeText(json) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, context.getString(R.string.share_export)))
                    } catch (e: Exception) { error = e.message ?: context.getString(R.string.error_generic) } finally { busy = false }
                }
            }
        }

        SectionTitle(stringResource(R.string.erase_account))
        Card(tint = TherapyColors.rose.copy(alpha = 0.15f)) {
            Muted(stringResource(R.string.erase_detail))
            preview?.let { p ->
                Text(stringResource(R.string.erase_removed), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                p.erased.forEach { Muted("• $it") }
                Text(stringResource(R.string.erase_kept), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                p.retained.forEach { Muted("• $it") }
                Muted(p.why, Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(10.dp))
            if (erased) Text(stringResource(R.string.erased_done), color = TherapyColors.critical)
            else TextButton({ confirmErase = true }) { Text(stringResource(R.string.erase_account), color = TherapyColors.critical) }
        }
        ErrorText(error)
    }

    if (confirmErase) AlertDialog(
        onDismissRequest = { confirmErase = false },
        title = { Text(stringResource(R.string.erase_confirm_title)) },
        text = { Text(stringResource(R.string.erase_confirm_body)) },
        confirmButton = {
            TextButton({
                confirmErase = false
                scope.launch {
                    try {
                        val outcome = container.api.requestErasure()
                        if (outcome.erased == true) { erased = true; container.api.signOut(); onErased() }
                        else error = outcome.reason ?: outcome.detail
                    } catch (e: Exception) { error = e.message }
                }
            }) { Text(stringResource(R.string.erase_confirm_action), color = TherapyColors.critical) }
        },
        dismissButton = { TextButton({ confirmErase = false }) { Text(stringResource(R.string.cancel)) } }
    )
}
