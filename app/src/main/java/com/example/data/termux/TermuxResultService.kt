package com.example.data.termux

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder

class TermuxResultService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleResultIntent(intent)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun handleResultIntent(intent: Intent) {
        intent.extras?.classLoader = classLoader
        val executionId = intent.getIntExtra("execution_id", -1)

        val resultBundle: Bundle? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getBundleExtra("result")
        } else {
            @Suppress("DEPRECATION")
            intent.getBundleExtra("result")
        }

        if (resultBundle == null) {
            TermuxExecutionBus.emitResult(
                TermuxCommandResult(
                    executionId = executionId,
                    stdout = "",
                    stderr = "",
                    exitCode = -1,
                    err = -1,
                    errmsg = "No result bundle returned from Termux. Ensure allow-external-apps=true in ~/.termux/termux.properties."
                )
            )
            BuildPersistenceService.notifyFinished(this, executionId, -1)
            return
        }

        resultBundle.classLoader = classLoader
        val stdout = resultBundle.getString("stdout") ?: ""
        val stderr = resultBundle.getString("stderr") ?: ""
        val exitCode = if (resultBundle.containsKey("exitCode")) {
            resultBundle.getInt("exitCode", 0)
        } else {
            resultBundle.getInt("exit_code", 0)
        }
        val err = resultBundle.getInt("err", 0)
        val errmsg = resultBundle.getString("errmsg")

        TermuxExecutionBus.emitResult(
            TermuxCommandResult(
                executionId = executionId,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                err = err,
                errmsg = errmsg
            )
        )
        BuildPersistenceService.notifyFinished(this, executionId, exitCode)
    }
}
