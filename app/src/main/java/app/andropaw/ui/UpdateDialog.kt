package app.andropaw.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.update.AppUpdateManager
import app.andropaw.update.ReleaseInfo
import app.andropaw.update.UpdateState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import java.io.File

/**
 * App-update dialogs, all on miuix [WindowDialog] (bottom sheet on phones). One per [UpdateState]
 * branch; the caller keeps the state and this file only presents it.
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onDismiss: () -> Unit,
    onStartDownload: (ReleaseInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onOpenUrl: (String) -> Unit = {},
    onRetry: (() -> Unit)? = null
) {
    when (state) {
        is UpdateState.Idle -> {}

        is UpdateState.Checking -> {
            MiuixDialog(
                show = true,
                title = stringResource(R.string.as_check_updates),
                summary = stringResource(R.string.update_checking),
                onDismissRequest = onDismiss,
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                DialogButtonRow(listOf(DialogAction(stringResource(R.string.cancel), onClick = onDismiss)))
            }
        }

        is UpdateState.Available -> {
            val rel = state.release
            MiuixDialog(
                show = true,
                title = stringResource(R.string.update_available_title),
                summary = stringResource(R.string.update_available_msg, rel.tagName),
                onDismissRequest = onDismiss,
            ) {
                Text(
                    text = if (rel.sizeBytes > 0) stringResource(R.string.update_size, AppUpdateManager.formatBytes(rel.sizeBytes)) else "",
                    fontSize = 12.sp,
                )
                if (rel.body.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp), insideMargin = PaddingValues(12.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(text = rel.body, fontSize = 12.sp)
                        }
                    }
                }
                DialogButtonRow(listOf(
                    DialogAction(stringResource(R.string.update_later), onClick = onDismiss),
                    DialogAction(stringResource(R.string.update_btn), onClick = { onStartDownload(rel) }, primary = true),
                ))
            }
        }

        is UpdateState.Downloading -> {
            val rel = state.release
            MiuixDialog(
                show = true,
                title = stringResource(R.string.update_downloading),
                onDismissRequest = onCancelDownload,
            ) {
                LinearProgressIndicator(
                    progress = if (state.totalBytes > 0) state.progress else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                val percent = (state.progress * 100).toInt()
                val downloadedStr = AppUpdateManager.formatBytes(state.downloadedBytes)
                val totalStr = if (state.totalBytes > 0) AppUpdateManager.formatBytes(state.totalBytes) else "…"
                Text(text = "$percent% ($downloadedStr / $totalStr)", fontSize = 12.sp)
                DialogButtonRow(listOf(DialogAction(stringResource(R.string.cancel), onClick = onCancelDownload)))
            }
        }

        is UpdateState.ReadyToInstall -> {
            MiuixDialog(
                show = true,
                title = stringResource(R.string.update_ready_title),
                summary = stringResource(R.string.update_ready_msg, state.release.tagName),
                onDismissRequest = onDismiss,
            ) {
                DialogButtonRow(listOf(
                    DialogAction(stringResource(R.string.close), onClick = onDismiss),
                    DialogAction(stringResource(R.string.update_install), onClick = { onInstall(state.file) }, primary = true),
                ))
            }
        }

        is UpdateState.UpToDate -> {
            MiuixDialog(
                show = true,
                title = stringResource(R.string.update_uptodate_title),
                summary = stringResource(R.string.update_latest, state.currentVersion),
                onDismissRequest = onDismiss,
            ) {
                if (state.latestTag.isNotBlank()) {
                    Text(text = stringResource(R.string.update_uptodate_github, state.latestTag), fontSize = 12.sp)
                }
                DialogButtonRow(listOf(
                    DialogAction(stringResource(R.string.update_open_github), onClick = { onOpenUrl(AppUpdateManager.GITHUB_REPO_URL) }),
                    DialogAction(stringResource(R.string.ok), onClick = onDismiss, primary = true),
                ))
            }
        }

        is UpdateState.Error -> {
            MiuixDialog(
                show = true,
                title = stringResource(R.string.update_error_title),
                summary = stringResource(R.string.update_error, state.message),
                onDismissRequest = onDismiss,
            ) {
                DialogButtonRow(buildList {
                    add(DialogAction(stringResource(R.string.close), onClick = onDismiss))
                    onRetry?.let { add(DialogAction(stringResource(R.string.retry), onClick = it, primary = true)) }
                })
            }
        }
    }
}
