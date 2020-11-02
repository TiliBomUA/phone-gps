package ua.org.gpstlka.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import ua.org.gpstlka.data.GpsPoint
import ua.org.gpstlka.data.StatusData
import ua.org.gpstlka.helpers.GPSFileUtils
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.IO)

    private val _imei = MutableLiveData<String>()
    val ldImei: LiveData<String> = _imei

    private val _isImeiEnabled = MutableLiveData<Boolean>()
    val ldIsImeiEnabled: LiveData<Boolean> = _isImeiEnabled

    private val _isPermissionLocationEnabled = MutableLiveData<Boolean>()
    val ldPermissionLocationEnabled: LiveData<Boolean> = _isPermissionLocationEnabled

    /*
     private val _isGpsEnabled = MutableLiveData<Boolean>()
     val ldIsGpsEnabled: LiveData<Boolean> = _isGpsEnabled

     private val _isNetWorkEnabled = MutableLiveData<Boolean>()
     val ldIsNetWorkEnabled: LiveData<Boolean> = _isNetWorkEnabled
 */
    private val _gpsData = MutableLiveData<GpsPoint>()
    val ldGpsData: LiveData<GpsPoint> = _gpsData

    private val _statusData = MutableLiveData<StatusData>()
    val ldStatusData: LiveData<StatusData> = _statusData

    fun setImei(newImei: String) {
        _imei.value = newImei
    }

    fun isPermisionImei(bool: Boolean) {
        _isImeiEnabled.value = bool
    }

    fun isPermissionLocationEnabled(bool: Boolean) {
        _isPermissionLocationEnabled.value = bool
    }

    fun sendDataToServer(path: File) {
        if (_imei.value.isNullOrEmpty()) {
            return
        }
        val listFilesSend = path.listFiles().filter { it.name.contains("_send") }
        if (listFilesSend.isEmpty()) {
            return
        }
        var lastTimeSend: String? = null
        viewModelScope.launch {
            listFilesSend.forEach {
                val ask = async { GPSFileUtils.sendOneFile(it, _imei.value!!) }
                val result = ask.await()

                val date: Date = Calendar.getInstance().time
                val sdf = SimpleDateFormat("dd_MM_yyyy HH:mm:ss", Locale.ROOT)
                val (fileCount, fileSize) = GPSFileUtils.getFileToSendInfo(path)
                if (result.Error == null) {
                    lastTimeSend = sdf.format(date)
                }

                withContext(Dispatchers.Main) {
                    _statusData.value = StatusData(fileCount, fileSize, lastTimeSend, result.Error)
                }
            }
        }
    }

    fun setGpsData(gps: GpsPoint) {
        _gpsData.value = gps
    }

    fun setNetStatus(status: StatusData) {
        _statusData.value = status
    }

    fun checkFileStatus(filesDir: File) {
        val (fileCount, fileSize) = GPSFileUtils.getFileToSendInfo(filesDir)
        _statusData.value = StatusData(fileCount, fileSize, null, null)
    }

}