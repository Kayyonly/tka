package com.tkasmp2027.security

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tkasmp2027.model.LockModeStatus

object ExamSecurityManager {

    fun checkLockModeStatus(context: Context): LockModeStatus {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (dpm != null && dpm.isLockTaskPermitted(context.packageName)) {
                LockModeStatus.FULL_LOCKDOWN_AVAILABLE
            } else {
                LockModeStatus.MONITORING_MODE_ONLY
            }
        } catch (_: Exception) {
            LockModeStatus.MONITORING_MODE_ONLY
        }
    }

    fun enterExamMode(activity: ComponentActivity): LockModeStatus {
        // 1. Protect exam screen content from screenshots, screen recording, and recent apps preview
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                activity.setRecentsScreenshotEnabled(false)
            } catch (_: Exception) {
                // Ignore if unsupported by device OEM
            }
        }

        // 2. Enable fullscreen immersive mode
        enableImmersiveMode(activity)

        // 3. Activate Lock Task Mode ONLY if officially permitted by Device Policy Controller (DPC)
        val lockStatus = checkLockModeStatus(activity)
        if (lockStatus == LockModeStatus.FULL_LOCKDOWN_AVAILABLE) {
            try {
                val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (am == null || am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    activity.startLockTask()
                }
            } catch (_: Exception) {
                return LockModeStatus.MONITORING_MODE_ONLY
            }
        }
        return lockStatus
    }

    fun exitExamMode(activity: ComponentActivity) {
        // 1. Clear FLAG_SECURE and restore recent-apps thumbnail behavior
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                activity.setRecentsScreenshotEnabled(true)
            } catch (_: Exception) {
                // Ignore if unsupported
            }
        }

        // 2. Restore system bars
        disableImmersiveMode(activity)

        // 3. Exit Lock Task Mode safely if currently locked
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am == null || am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.stopLockTask()
            }
        } catch (_: Exception) {
            // Safe no-op if not in lock task mode
        }
    }

    fun enableImmersiveMode(activity: ComponentActivity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun disableImmersiveMode(activity: ComponentActivity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}
