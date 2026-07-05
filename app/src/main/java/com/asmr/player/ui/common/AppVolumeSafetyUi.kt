package com.asmr.player.ui.common

import androidx.compose.ui.res.stringResource
import com.asmr.player.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.asmr.player.playback.AppVolume
import com.asmr.player.playback.AppVolumeChangeSource
import com.asmr.player.playback.AppVolumeSafety

data class PendingAppVolumeChange(
    val currentPercent: Int,
    val targetPercent: Int
)

@Stable
class AppVolumeWarningSessionState internal constructor() {
    var hasAcknowledgedWarningThisLaunch by mutableStateOf(false)
        private set

    fun markWarningAcknowledged() {
        hasAcknowledgedWarningThisLaunch = true
    }
}

@Stable
class ProtectedAppVolumeChangeState internal constructor(
    private val warningSessionState: AppVolumeWarningSessionState,
    private val onApplyVolumeChange: (Int) -> Unit
) {
    var pendingChange by mutableStateOf<PendingAppVolumeChange?>(null)
        private set

    fun requestVolumeChange(
        currentPercent: Int,
        targetPercent: Int,
        source: AppVolumeChangeSource
    ) {
        val current = AppVolume.clampPercent(currentPercent)
        val target = AppVolume.clampPercent(targetPercent)
        if (
            AppVolumeSafety.shouldWarnBeforeLoudVolume(
                targetPercent = target,
                source = source,
                hasAcknowledgedWarningThisLaunch = warningSessionState.hasAcknowledgedWarningThisLaunch
            )
        ) {
            pendingChange = PendingAppVolumeChange(currentPercent = current, targetPercent = target)
            return
        }
        pendingChange = null
        onApplyVolumeChange(target)
    }

    fun confirmPendingChange() {
        val change = pendingChange ?: return
        pendingChange = null
        warningSessionState.markWarningAcknowledged()
        onApplyVolumeChange(change.targetPercent)
    }

    fun dismissPendingChange() {
        pendingChange = null
    }
}

@Composable
fun rememberAppVolumeWarningSessionState(): AppVolumeWarningSessionState {
    return remember { AppVolumeWarningSessionState() }
}

@Composable
fun rememberProtectedAppVolumeChangeState(
    warningSessionState: AppVolumeWarningSessionState,
    onApplyVolumeChange: (Int) -> Unit
): ProtectedAppVolumeChangeState {
    val currentOnApply = rememberUpdatedState(onApplyVolumeChange)
    return remember(warningSessionState) {
        ProtectedAppVolumeChangeState(warningSessionState) { percent ->
            currentOnApply.value(percent)
        }
    }
}

@Composable
fun AppVolumeHearingWarningDialog(
    state: ProtectedAppVolumeChangeState
) {
    val pendingChange = state.pendingChange ?: return
    FlatActionDialog(
        onDismissRequest = state::dismissPendingChange,
        message = stringResource(R.string.str_e1596487),
        actions = listOf(
            FlatDialogAction("取消", state::dismissPendingChange),
            FlatDialogAction("确认", state::confirmPendingChange, FlatDialogActionTone.Primary)
        )
    )
}
