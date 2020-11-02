package ua.org.gpstlka.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import ua.org.gpstlka.MainActivity
import ua.org.gpstlka.helpers.GPSFileUtils
import java.io.*
import java.text.SimpleDateFormat
import ua.org.gpstlka.R
import ua.org.gpstlka.data.*
import java.util.*
import kotlin.math.round

class GpsService : Service() {

    val CHANNEL_ID_DEFOULT = "GPSTLKA_SERVICE"
    val NATIFICATION_ID = 2020

    private lateinit var channelId: String

    private var locationRequest: LocationRequest? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    private var saveCurDistanse = 0f
    private var saveCurTime = 0L
    private lateinit var saveStartTime: Date

    private lateinit var imei: String
    private lateinit var setting: SettingApp

    private var prevLocation: Location? = null
    private var allDistance = 0f
    private var lastTimeSend: String? = null

    private lateinit var timeExit: Date

    private val serJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(serJob + Dispatchers.IO)

    private val listSaveLocation = mutableListOf<String>()


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        imei = intent!!.getStringExtra("imei") ?: ""
        setting = intent.getParcelableExtra("appSetting")?: return START_STICKY

        if (imei.isEmpty()) {
            stopSelf()
            return START_STICKY
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeExit = sdf.parse(setting!!.timeAutoExit)

        saveStartTime = Calendar.getInstance().time
        channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                CHANNEL_ID_DEFOULT
            }
        createNatification("Start service")
        locationRequest = LocationRequest()
        startLocationListen()
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        mFusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }

    private fun startLocationListen() {

        // Create the location request to start receiving updates
        if (locationRequest == null) {
            return
        }

        val intervalSec = setting.interval * 1000L
        val intervalFastSec = setting.fastesInterval * 1000L
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = intervalSec
        locationRequest!!.fastestInterval = intervalFastSec
        locationRequest!!.smallestDisplacement = setting.minDistanceBetweenUpdates

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation

            val lat = roundedLatLong(lastLocation.latitude)
            val long = roundedLatLong(lastLocation.longitude)
            val dateTime = lastLocation.time
            val speed = getMySpeed(lastLocation.speed)
            val date: Date = Calendar.getInstance().time
            date.time = lastLocation.time
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val sdf_tocave = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

            val dateNow = Calendar.getInstance().time

            var dist = 0f
            prevLocation?.let {
                dist = lastLocation.distanceTo(it)
            }

            prevLocation = lastLocation
            allDistance += dist

            val locationManager = baseContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isWiFiEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var statusLoc = "No"
            if (isGpsEnabled) {
                statusLoc = "GPS"
            } else if (isWiFiEnabled) {
                statusLoc = "WiFi"
            }

            val intent = Intent(Constant.FILTER_NAME)
            intent.putExtra(Constant.NAME_EXTRA_DATA, GpsPoint(sdf.format(date), lat, long, speed, allDistance, statusLoc))
            sendBroadcast(intent)

            if ((lat > 1) && (long > 1) && (dateTime > 1000)) {
                listSaveLocation.add(sdf.format(date) + ";" + lat + ";" + long + ";$speed")
            }

            createNatification("Stat: $statusLoc , dis:$allDistance m")

            val testLastTime = Date()
            testLastTime.time = 0
            testLastTime.hours = date.hours
            testLastTime.minutes = date.minutes
            testLastTime.seconds = date.seconds

            if (setting.isAutoExit) {
                if (testLastTime.after(timeExit)) {
                    saveListToStorage(sdf_tocave.format(date))
                    sendToServ()
                    stopSelf()
                }
            }

            if (listSaveLocation.size > 10) {
                saveListToStorage(sdf_tocave.format(date))
            }

            saveCurTime = dateNow.time - saveStartTime.time
            val timeTestinMin = setting.maxTimeForSend * 60000
            //triger to send on Server
            if ((saveCurDistanse > setting.maxDistanceForSend) || (saveCurTime > timeTestinMin)) {
                sendToServ()
            }
        }
    }

    fun roundedLatLong(incoming: Double): Double {
        val dec = 100000
        val full = incoming * dec
        val rounded = round(full)
        return rounded / dec
    }

    fun getMySpeed(speed: Float): Float {
        val full = speed * 10
        val rounded = round(full)
        return rounded / 10
    }

    private fun saveListToStorage(fileName: String) {
        val file = File(filesDir, fileName)

        val strToWrite = listSaveLocation.joinToString(separator = "\n") + "\n"
        file.appendText(strToWrite)

        val fileToSend = File(filesDir, fileName + "_send")
        fileToSend.appendText(strToWrite)
        listSaveLocation.clear()

        val (fileCount, fileSize) = GPSFileUtils.getFileToSendInfo(filesDir)

        val intent = Intent(Constant.FILTER_NAME)
        intent.putExtra(Constant.NAME_EXTRA_NETSTATUS, StatusData(fileCount, fileSize, lastTimeSend, null))
        sendBroadcast(intent)
    }

    private fun sendToServ() {
        val (fileCount, fileSize) = GPSFileUtils.getFileToSendInfo(filesDir)
        if (fileCount < 1) {
            val intent = Intent(Constant.FILTER_NAME)
            intent.putExtra(Constant.NAME_EXTRA_NETSTATUS, StatusData(fileCount, fileSize, lastTimeSend, "No Files"))
            sendBroadcast(intent)
            return
        }

        val netActiveStatus = GPSFileUtils.getNetWorkStatus(this)
        val isNetConnect = netActiveStatus.first
        if (!isNetConnect) {
            val intent = Intent(Constant.FILTER_NAME)
            intent.putExtra(Constant.NAME_EXTRA_NETSTATUS, StatusData(fileCount, fileSize, lastTimeSend, "No internet"))
            sendBroadcast(intent)
            return
        }

        saveCurDistanse = 0f
        saveStartTime = Calendar.getInstance().time

        //find file to send and try to send to server
        val listFilesSend = filesDir.listFiles().filter { it.name.contains("_send") }
        //try send
        viewModelScope.launch {
            listFilesSend.forEach {
                val ask = async { GPSFileUtils.sendOneFile(it, imei) }
                val result = ask.await()

                val date: Date = Calendar.getInstance().time
                val sdf = SimpleDateFormat("dd_MM_yyyy HH:mm:ss", Locale.getDefault())

                val (fileCount, fileSize) = GPSFileUtils.getFileToSendInfo(filesDir)
                val intent = Intent(Constant.FILTER_NAME)
                if (result.Error == null) {
                    lastTimeSend = sdf.format(date)
                }
                intent.putExtra(Constant.NAME_EXTRA_NETSTATUS, StatusData(fileCount, fileSize, lastTimeSend, result.Error))
                sendBroadcast(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = CHANNEL_ID_DEFOULT
        val channelName = "My Background Service"
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun createNatification(text: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val natification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("GPS  TLKA")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NATIFICATION_ID, natification)
    }

}
