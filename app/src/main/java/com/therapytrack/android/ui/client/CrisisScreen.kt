package com.therapytrack.android.ui.client

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.R
import com.therapytrack.android.clinical.CrisisResources
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.theme.TherapyColors

@Composable
fun CrisisScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.crisis_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Muted(stringResource(R.string.crisis_intro))
        CrisisResources.resources().forEach { r ->
            Card(tint = if (r.isEmergency) TherapyColors.rose.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface) {
                Text(r.name, style = MaterialTheme.typography.titleMedium)
                Text(r.number, style = MaterialTheme.typography.headlineMedium, color = if (r.isEmergency) TherapyColors.critical else TherapyColors.navy)
                Muted(r.detail)
                Spacer(Modifier.height(10.dp))
                // ACTION_DIAL opens the dialler with the number filled in; the person places the call.
                PrimaryButton("${stringResource(R.string.call)} ${r.number}") { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${r.dialable}"))) }
            }
        }
        if (CrisisResources.needsInternationalDirectory()) {
            TextButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CrisisResources.INTERNATIONAL_DIRECTORY))) }) { Text(stringResource(R.string.crisis_international)) }
        }
        Muted(stringResource(R.string.crisis_therapist_note))
    }
}
