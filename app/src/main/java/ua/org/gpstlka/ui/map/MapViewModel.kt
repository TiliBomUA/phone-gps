package ua.org.gpstlka.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import ua.org.gpstlka.data.LocalTrack
import ua.org.gpstlka.helpers.GPSFileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MapViewModel : ViewModel() {

    private val serJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(serJob + Dispatchers.IO)

    private val _track = MutableLiveData<LocalTrack>()
    val ldTrack: LiveData<LocalTrack> = _track

    fun loadTrack(curDate: Calendar, filesDir: File) {
        viewModelScope.launch {

            val fileFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
            val nameFile = fileFormat.format(curDate.time)
            val fullPath = filesDir.path + "/" + nameFile
            val file = File(fullPath)
            val fileExists = file.exists()
            if (!fileExists) {
                withContext(Dispatchers.Main) {
                    _track.value = LocalTrack(false, null)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _track.value = GPSFileUtils.loadFromFile(file)
            }
        }
    }
}