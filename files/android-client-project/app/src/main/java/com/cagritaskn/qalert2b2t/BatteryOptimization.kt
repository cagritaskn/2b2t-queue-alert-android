package com.cagritaskn.qalert2b2t

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.PowerManager

class BatteryOptimization(private val activity: Activity) {

    fun checkAndPromptBatteryOptimization() {
        if (isBatteryOptimizationEnabled(activity)) {
            showBatteryOptimizationDialog(activity)
        }
    }

    private fun isBatteryOptimizationEnabled(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            false
        }
    }

    private fun showBatteryOptimizationDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Disable Battery Optimization")
        builder.setMessage("You need to turn battery optimization off for this app to make it function properly. If you can't find the 2B2T Queue Alert app on the list click on Not optimized at the top of the window and select All apps then find the 2B2T Queue Alert app there and disable the optimization.")
        builder.setPositiveButton("GO TO SETTINGS") { _, _ ->
            goToBatteryOptimizationSettings(context)
        }
        builder.setNegativeButton("QUIT") { _, _ ->
            activity.finish()
        }
        builder.setOnCancelListener {
            activity.finish()
        }
        builder.create().show()
    }

    private fun goToBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
        }
    }
}
