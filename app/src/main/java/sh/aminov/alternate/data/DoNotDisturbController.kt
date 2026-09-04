package sh.aminov.alternate.data

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Turns Do Not Disturb on for the duration of a session.
 *
 * The one real fix for the notification problem: a heads-up notification does
 * not stop the activity, so no lifecycle callback catches it — it lands on the
 * pad, takes the touch focus, and the run is over. Android will not let an app
 * do this quietly, so the user grants notification policy access once in system
 * settings; without the grant everything here is a no-op. The previous filter
 * is remembered and restored, so someone who lives in "priority only" gets
 * their own setting back rather than ours.
 */
class DoNotDisturbController(private val context: Context) {

    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private var previousFilter: Int? = null

    /** True when the user has granted notification policy access. */
    val isGranted: Boolean
        get() = notificationManager?.isNotificationPolicyAccessGranted == true

    /** The system screen where the grant is made. */
    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Silences everything. Does nothing without the grant, and nothing if a
     * session is already holding the filter.
     */
    fun engage() {
        val manager = notificationManager ?: return
        if (!isGranted || previousFilter != null) return

        runCatching {
            previousFilter = manager.currentInterruptionFilter
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }.onFailure { previousFilter = null }
    }

    /** Puts back whatever the filter was before [engage]. */
    fun release() {
        val manager = notificationManager ?: return
        val restore = previousFilter ?: return
        previousFilter = null
        if (!isGranted) return

        runCatching { manager.setInterruptionFilter(restore) }
    }
}
