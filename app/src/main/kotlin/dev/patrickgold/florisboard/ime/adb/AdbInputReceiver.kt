package dev.patrickgold.florisboard.ime.adb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputConnection
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.devtools.flogWarning

/**
 * Broadcast receiver that allows shell/ADB clients to commit text into the current editor while
 * FlorisBoard is the active input method. This makes it easy to feed characters or emoji via
 * `adb shell am broadcast` without going through the on-screen UI.
 */
class AdbInputReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }
        when (intent.action) {
            ACTION_COMMIT_TEXT -> handleCommitText(intent)
            else -> {
                flogWarning(LogTopic.IMS_EVENTS) {
                    "Unsupported action '${intent.action}' sent to ${javaClass.simpleName}"
                }
            }
        }
    }

    private fun handleCommitText(intent: Intent) {
        val rawText = intent.getStringExtra(EXTRA_TEXT)
        if (rawText.isNullOrEmpty()) {
            flogWarning(LogTopic.IMS_EVENTS) {
                "Ignoring ${ACTION_COMMIT_TEXT} broadcast without '${EXTRA_TEXT}' extra"
            }
            return
        }
        val ic: InputConnection = FlorisImeService.currentInputConnection() ?: run {
            flogWarning(LogTopic.IMS_EVENTS) {
                "Unable to commit text '$rawText' because no input connection is active"
            }
            return
        }
        val cursorDelta = intent.getIntExtra(
            EXTRA_CURSOR_POSITION,
            rawText.codePointCount(0, rawText.length),
        )
        ic.commitText(rawText, cursorDelta)
        flogInfo(LogTopic.IMS_EVENTS) { "Committed text via ADB: '$rawText'" }
    }

    companion object {
        const val ACTION_COMMIT_TEXT = "dev.patrickgold.florisboard.action.COMMIT_TEXT"
        const val EXTRA_TEXT = "text"
        const val EXTRA_CURSOR_POSITION = "cursor_position"
    }
}
