package com.cagritaskn.qalert2b2t

import android.annotation.SuppressLint import android.app.* import android.content.Context import android.content.Intent import android.content.pm.PackageManager import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.RingtoneManager import android.os.Build import android.os.Bundle import android.os.Handler import android.os.Looper
import android.util.Log import android.view.KeyEvent import android.view.View import android.view.inputmethod.InputMethodManager import android.view.animation.AlphaAnimation import android.view.inputmethod.EditorInfo import android.widget.* import androidx.appcompat.app.AppCompatActivity import androidx.core.app.NotificationCompat import androidx.core.app.NotificationManagerCompat import androidx.core.content.ContextCompat import org.json.JSONObject import java.io.BufferedReader import java.io.InputStreamReader import java.net.HttpURLConnection import java.net.URL import org.json.JSONException
import android.text.method.LinkMovementMethod
import android.text.SpannableString
import android.text.util.Linkify
import android.view.Gravity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var updateDataRunnable: Runnable? = null
    private lateinit var ipPart1: EditText
    private lateinit var ipPart2: EditText
    private lateinit var ipPart3: EditText
    private lateinit var ipPart4: EditText
    private lateinit var numericValueTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var connectButton: Button
    private lateinit var notifyRestartSwitch: Switch
    private lateinit var notificationMinimumCountPicker: NumberPicker


    private var notifyRestartEnabled = false
    private var notificationMinimumCount = 10
    private var notificationSent = false

    private var queueAlertLastSentTime = 0L
    private val QUEUE_ALERT_INTERVAL_MS = 30000L // 60 seconds in milliseconds
    private val INITIAL_DELAY_MS = 30000L // 60 seconds in milliseconds

    companion object {
        private const val TAG = "MainActivity"
        private const val BASE_URL = "http://%s:5000/get-data"
        private const val REFRESH_INTERVAL_MS = 500L
        private const val NOTIFICATION_CHANNEL_ID = "server_notifications"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_INTERVAL_MS = 10000L
        private const val QUEUE_ALERT_NOTIFICATION_ID = 2
        private const val PERMISSION_REQUEST_CODE = 1001
        const val NOTIFICATION_ACTION_OPEN = "OPEN_NOTIFICATIONS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceIntent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        ipPart1 = findViewById(R.id.ip_part1)
        ipPart2 = findViewById(R.id.ip_part2)
        ipPart3 = findViewById(R.id.ip_part3)
        ipPart4 = findViewById(R.id.ip_part4)
        numericValueTextView = findViewById(R.id.numeric_value_text_view)
        statusTextView = findViewById(R.id.status_text_view)
        connectButton = findViewById(R.id.connect_button)
        notifyRestartSwitch = findViewById(R.id.notify_restart_switch)
        notificationMinimumCountPicker = findViewById(R.id.notification_minimum_count_picker)

        val backgroundDrawable = ContextCompat.getDrawable(this, R.drawable.edit_text_background)
        connectButton.background = backgroundDrawable
        connectButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        setupNumberPicker()

        notifyRestartSwitch.setOnCheckedChangeListener { _, isChecked ->
            notifyRestartEnabled = isChecked
            if (notifyRestartEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_CODE
                    )
                }
            }
        }

        connectButton.setOnClickListener {
            handleConnectButtonClick()
        }

        setEditorActionListener(ipPart1)
        setEditorActionListener(ipPart2)
        setEditorActionListener(ipPart3)
        setEditorActionListener(ipPart4)

        // IP adresini yükle ve gir
        loadSavedIpAddress()?.let { ipAddress ->
            val ipParts = ipAddress.split(".")
            if (ipParts.size == 4) {
                ipPart1.setText(ipParts[0])
                ipPart2.setText(ipParts[1])
                ipPart3.setText(ipParts[2])
                ipPart4.setText(ipParts[3])
            }
            startFetchingData(ipAddress)
            showStatusTextFor5Seconds()
        }

        // Bildirim engelleme süresi
        handler.postDelayed({
            queueAlertLastSentTime = System.currentTimeMillis() - QUEUE_ALERT_INTERVAL_MS
        }, INITIAL_DELAY_MS)

        // Notification Channel oluştur
        createNotificationChannel()

        val creditsButton: ImageButton = findViewById(R.id.credits_button)
        creditsButton.setOnClickListener {
            showCreditsDialog()
        }

        val helpButton: ImageButton = findViewById(R.id.help_button)

        helpButton.setOnClickListener {
            showHelpDialog()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.statusBarColor)
        }


    }

    private fun setupNumberPicker() {
        val values = arrayOf("Queue Notifications Off", "5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "100")

        notificationMinimumCountPicker.minValue = 0
        notificationMinimumCountPicker.maxValue = values.size - 1
        notificationMinimumCountPicker.displayedValues = values
        notificationMinimumCountPicker.wrapSelectorWheel = true

        notificationMinimumCountPicker.setOnValueChangedListener { _, _, newValue ->
            notificationMinimumCount = if (newValue == 0) {
                0 // "Queue Notifications Off" seçildiğinde değeri 0 olarak ayarla
            } else {
                values[newValue].toInt()
            }
        }

        setNumberPickerTextColor(notificationMinimumCountPicker, Color.WHITE)
    }

    private fun setNumberPickerTextColor(numberPicker: NumberPicker, color: Int) {
        try {
            val count = numberPicker.childCount
            for (i in 0 until count) {
                val child = numberPicker.getChildAt(i)
                if (child is EditText) {
                    try {
                        child.setTextColor(color)
                        child.invalidate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting NumberPicker text color", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing NumberPicker children", e)
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Server Notifications"
            val descriptionText = "Notifications for server status and restarts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(title: String, content: String, notificationId: Int) {
        // Check if notification permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
                return
            }
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlag)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    private fun handleConnectButtonClick() {
        val ipAddress = getIpAddress()

        // Bildirim iznini kontrol et ve gerekirse izin iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                // İzin zaten verilmişse, veriyi çekmeye başla
                startFetchingData(ipAddress)
            }
        } else {
            // Eğer Android sürümü TIRAMISU'dan düşükse izne gerek yok, veriyi çekmeye başla
            startFetchingData(ipAddress)
        }

        showStatusTextFor5Seconds()
        hideKeyboard()
    }

    private fun getIpAddress(): String {
        val part1 = ipPart1.text.toString().takeIf { it.isNotEmpty() } ?: "0"
        val part2 = ipPart2.text.toString().takeIf { it.isNotEmpty() } ?: "0"
        val part3 = ipPart3.text.toString().takeIf { it.isNotEmpty() } ?: "0"
        val part4 = ipPart4.text.toString().takeIf { it.isNotEmpty() } ?: "0"
        return "$part1.$part2.$part3.$part4"
    }

    private fun startFetchingData(ipAddress: String) {
        stopFetchingData()
        updateDataRunnable = object : Runnable {
            override fun run() {
                fetchData(ipAddress)
                handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }
        handler.post(updateDataRunnable!!)
    }

    private fun stopFetchingData() {
        updateDataRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    private fun fetchData(ipAddress: String) {
        Thread {
            try {
                val url = URL(String.format(BASE_URL, ipAddress))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                connection.disconnect()
                parseAndUpdateUI(response.toString(), ipAddress)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                runOnUiThread {
                    numericValueTextView.text = "N/A"
                    showStatusMessage("Couldn't connect to the server.")
                }
            }
        }.start()
    }

    private fun parseAndUpdateUI(response: String, ipAddress: String) {
        runOnUiThread {
            try {
                val jsonResponse = JSONObject(response)
                val numericValueString = jsonResponse.optString("numeric_value", null)
                val isRestarting = jsonResponse.optBoolean("is_restarting", false)

                val numericValue = numericValueString?.toIntOrNull()

                when {
                    numericValue == null || numericValue > 1000 -> {
                        numericValueTextView.text = "Join 2B2T"
                    }

                    numericValueString == "N/A" -> {
                        numericValueTextView.text = "N/A"
                    }

                    else -> {
                        numericValueTextView.text = numericValueString
                    }
                }

                if (numericValue != null && numericValue in 1..1000 && numericValue <= notificationMinimumCount) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - queueAlertLastSentTime >= QUEUE_ALERT_INTERVAL_MS) {
                        sendNotification(
                            "Queue Alert",
                            "Picked queue limit has been reached.\nYour position in queue is $numericValue",
                            QUEUE_ALERT_NOTIFICATION_ID
                        )
                        queueAlertLastSentTime = currentTime
                    }
                }

                if (isRestarting && notifyRestartEnabled) {
                    sendNotification(
                        "Server Restarting",
                        "The server is going to be restarted soon.",
                        NOTIFICATION_ID
                    )
                    notifyRestartEnabled = false
                    notifyRestartSwitch.isChecked = false
                }

                // IP adresini başarılı bir şekilde bağlandıktan sonra kaydet
                saveIpAddress(ipAddress)
                showStatusMessage("Connected successfully.")
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing JSON", e)
                showStatusMessage("Couldn't connect to the server.")
            }
        }
    }

    private fun showStatusTextFor5Seconds() {
        statusTextView.visibility = View.VISIBLE
        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeOut.duration = 2000
        fadeOut.startOffset = 3000
        fadeOut.fillAfter = true
        statusTextView.startAnimation(fadeOut)
    }

    private fun showStatusMessage(message: String) {
        statusTextView.text = message
        statusTextView.visibility = View.VISIBLE
    }

    private fun showHelpDialog() {
        val builder = AlertDialog.Builder(this)

        // TextView ile başlığı ayarlıyoruz
        val titleTextView = TextView(this)
        titleTextView.text = "Help"
        titleTextView.setPadding(10, 40, 10, 20)
        titleTextView.gravity = Gravity.CENTER // Yatayda ortala
        titleTextView.setTextColor(Color.WHITE) // Beyaz renk
        titleTextView.textSize = 20f // Yazı boyutu
        titleTextView.setTypeface(null, Typeface.BOLD) // Kalın yazı

        // Başlığı dialog'a ekliyoruz
        builder.setCustomTitle(titleTextView)

        // Mesajın içeriği
        val message = SpannableString("This app tracks and notifies you of your 2B2T queue position. To get started, first run the server on your PC and enter the IP address provided by the PC into the app's IP address input field. Be sure to grant notification permissions to ensure the app functions correctly.\nFor more details, visit:\nhttps://github.com/cagritaskn/2b2t-queue-alert-android")

        // Linkify kullanarak URL'yi tıklanabilir hale getir
        Linkify.addLinks(message, Linkify.WEB_URLS)

        // Mesajı bir TextView içinde ayarlıyoruz
        val messageTextView = TextView(this)
        messageTextView.text = message
        messageTextView.movementMethod = LinkMovementMethod.getInstance()
        messageTextView.setTextColor(Color.WHITE) // Yazıları beyaz yap
        messageTextView.setPadding(20, 30, 20, 30) // Padding ekle

        // TextView'i dialog'a ekle
        builder.setView(messageTextView)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        // Diyalog kutusunu oluştur ve göster
        val dialog = builder.create()

        // Diyalog arka planını koyu gri yap
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.DKGRAY))

        dialog.show()
    }

    private fun showCreditsDialog() {
        val builder = AlertDialog.Builder(this)

        // Set up the title TextView
        val titleTextView = TextView(this)
        titleTextView.text = "Credits"
        titleTextView.setPadding(10, 30, 10, 20)
        titleTextView.gravity = Gravity.CENTER
        titleTextView.setTextColor(Color.WHITE)
        titleTextView.textSize = 20f
        titleTextView.setTypeface(null, Typeface.BOLD)

        // Set the custom title
        builder.setCustomTitle(titleTextView)

        // Message content
        val message = SpannableString("© 2024 Çağrı Taşkın. 2B2T Queue Alert.")

        // Set the message in a TextView
        val messageTextView = TextView(this)
        messageTextView.gravity = Gravity.CENTER
        messageTextView.text = message
        messageTextView.setTextColor(Color.WHITE)
        messageTextView.setPadding(20, 40, 20, 30)

        // Add the TextView to the dialog
        builder.setView(messageTextView)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.DKGRAY))
        dialog.show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(connectButton.windowToken, 0)
    }

    private fun setEditorActionListener(editText: EditText) {
        editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard()
                v.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun saveIpAddress(ipAddress: String) {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ip_address", ipAddress)
        editor.apply()
    }

    private fun loadSavedIpAddress(): String? {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        return sharedPreferences.getString("ip_address", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFetchingData()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Notification permission is required to send notifications",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}