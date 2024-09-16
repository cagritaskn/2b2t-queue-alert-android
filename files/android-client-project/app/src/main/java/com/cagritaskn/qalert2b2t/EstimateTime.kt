package com.cagritaskn.qalert2b2t

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EstimateTime(private val activity: AppCompatActivity) {
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private lateinit var numericValueTextView: TextView
    private lateinit var estimatedWaitTimeTextView: TextView

    fun startUpdating() {
        numericValueTextView = activity.findViewById(R.id.numeric_value_text_view)
        estimatedWaitTimeTextView = activity.findViewById(R.id.estimated_wait_time)

        updateRunnable = object : Runnable {
            override fun run() {
                updateEstimatedTime()
                handler.postDelayed(this, 500L) // Update interval in milliseconds
            }
        }
        handler.post(updateRunnable!!)
    }

    fun stopUpdating() {
        updateRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    private fun updateEstimatedTime() {
        activity.runOnUiThread {
            val numericValueString = numericValueTextView.text.toString()
            val numericValue = numericValueString.toIntOrNull()

            if (numericValue != null && numericValue in 1..1500) {
                // Calculate the estimated wait time in minutes
                val estimatedMinutes = numericValue / 1.1
                val hours = (estimatedMinutes / 60).toInt()
                val minutes = (estimatedMinutes % 60).toInt()

                // Format the time as HH:MM
                val formattedTime = String.format("%02d:%02d", hours, minutes)
                estimatedWaitTimeTextView.text = formattedTime
            } else {
                // Set to "N/A" if the value is not in the valid range
                estimatedWaitTimeTextView.text = "N/A"
            }
        }
    }
}
