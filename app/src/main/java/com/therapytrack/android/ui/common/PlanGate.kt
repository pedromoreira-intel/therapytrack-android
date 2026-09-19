package com.therapytrack.android.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiError
import com.therapytrack.android.ui.theme.TherapyColors

/**
 * What a screen shows when the server answered 402. The feature is not hidden
 * by guessing the plan locally: the server is the only authority on what is
 * included, and its message is what the person reads.
 */
@Composable
fun NotInPlanCard(error: ApiError.NotInPlan, onSeePlan: () -> Unit) {
    Card(tint = TherapyColors.champagne.copy(alpha = 0.35f)) {
        Text(stringResource(R.string.not_in_plan_title), style = MaterialTheme.typography.titleMedium, color = TherapyColors.navy)
        Muted(error.message ?: "", Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
            TextButton(onSeePlan) { Text(stringResource(R.string.see_plan)) }
        }
    }
}

/** A load result a screen can render in one `when`. */
sealed class Loaded<out T> {
    object Loading : Loaded<Nothing>()
    data class Ok<T>(val value: T) : Loaded<T>()
    data class NotInPlan(val error: ApiError.NotInPlan) : Loaded<Nothing>()
    data class Failed(val error: Throwable) : Loaded<Nothing>()
}

suspend fun <T> load(block: suspend () -> T): Loaded<T> = try {
    Loaded.Ok(block())
} catch (e: ApiError.NotInPlan) {
    Loaded.NotInPlan(e)
} catch (e: Exception) {
    Loaded.Failed(e)
}
