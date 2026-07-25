package org.bileichat.mesh

import android.app.Application
import android.content.Context

class MeshApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("crash_log", Context.MODE_PRIVATE)

        // Surface the previous run's crash (if any) into the debug log, then clear it.
        prefs.getString("last_crash", null)?.let {
            MeshState.logDebug("LAST CRASH:\n$it")
            prefs.edit().remove("last_crash").apply()
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                prefs.edit()
                    .putString(
                        "last_crash",
                        "${System.currentTimeMillis()} ${thread.name}: ${android.util.Log.getStackTraceString(e)}".take(4000)
                    )
                    .commit() // synchronous — process is about to die
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, e)
        }
    }
}
