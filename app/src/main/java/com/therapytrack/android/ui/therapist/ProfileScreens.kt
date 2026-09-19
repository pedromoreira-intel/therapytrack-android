package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.BuildConfig
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiUser
import com.therapytrack.android.ui.common.ListCard
import com.therapytrack.android.ui.common.ListRow
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.ProfileHeader
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TherapistProfileScreen(onSignedOut: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var me by remember { mutableStateOf<ApiUser?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val plan by container.plan.collectAsState()
    LaunchedEffect(Unit) { runCatching { me = container.api.me() }; container.refreshPlan() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileHeader(me?.name ?: "", stringResource(R.string.role_therapist), me?.email ?: "", stringResource(R.string.role_therapist))
        Overline(stringResource(R.string.personal_info), modifier = Modifier.padding(top = 12.dp))
        ListCard {
            ListRow(stringResource(R.string.name), trailing = me?.name ?: "")
            ListRow(stringResource(R.string.email), trailing = me?.email ?: "", divider = false)
        }
        Overline(stringResource(R.string.plan_section), modifier = Modifier.padding(top = 12.dp))
        PlanCard(plan)
        Overline(stringResource(R.string.app_settings), modifier = Modifier.padding(top = 12.dp))
        ListCard {
            ListRow(stringResource(R.string.language_setting), trailing = Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercase() }, icon = Icons.Outlined.Language, divider = false)
        }
        Overline(stringResource(R.string.data_privacy), modifier = Modifier.padding(top = 12.dp))
        ListCard { ListRow(stringResource(R.string.sign_out), icon = Icons.AutoMirrored.Outlined.Logout, tint = TherapyColors.critical, titleColor = TherapyColors.critical, divider = false) { confirm = true } }
        Overline(stringResource(R.string.app_info), modifier = Modifier.padding(top = 12.dp))
        ListCard { ListRow(stringResource(R.string.version_label), trailing = BuildConfig.VERSION_NAME, divider = false) }
        Spacer(Modifier.height(8.dp))
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.sign_out)) }, text = { Text(stringResource(R.string.sign_out_confirm)) },
        confirmButton = { TextButton({ confirm = false; scope.launch { container.api.signOut(); onSignedOut() } }) { Text(stringResource(R.string.sign_out), color = TherapyColors.critical) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
