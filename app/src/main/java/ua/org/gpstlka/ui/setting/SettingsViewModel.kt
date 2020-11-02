package ua.org.gpstlka.ui.setting

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ua.org.gpstlka.data.Constant
import ua.org.gpstlka.data.SettingApp

class SettingsViewModel(private val sharPref: SharedPreferences) : ViewModel() {


    private val _settings = MutableLiveData<SettingApp>().apply {
        value = refreshSettingsData()
    }

    val ldSettings: LiveData<SettingApp> = _settings

    fun setSettings(
        timeInterval: Int?,
        timeIntervalFast: Int?,
        distanceMaxInterval: Float?,
        dataSendTimeValue: Int?,
        dataSendDistance: Float?,
        autoExit: Boolean?,
        timeAutoEXit: String?
    ) {

        val editor = sharPref.edit()
        timeInterval?.let {
            editor.putInt("interval", it)
        }

        timeIntervalFast?.let {
            editor.putInt("fastesInterval", it)
        }

        distanceMaxInterval?.let {
            editor.putFloat("minDistanceBetweenUpdates", it)
        }

        dataSendTimeValue?.let {
            editor.putInt("maxTimeForSend", it)
        }

        dataSendDistance?.let {
            editor.putFloat("maxDistanceForSend", it)
        }

        autoExit?.let {
            editor.putBoolean("isAutoExit", it)
        }

        timeAutoEXit?.let {
            editor.putString("timeAutoExit", it)
        }
        editor.apply()
        _settings.value = refreshSettingsData()
    }

    private fun refreshSettingsData(): SettingApp {
        val interval = sharPref.getInt("interval", Constant.INTERVAL)
        val fastesInterval = sharPref.getInt("fastesInterval", Constant.FASTEST_INTERVAL)
        val minDistanceBetweenUpdates = sharPref.getFloat("minDistanceBetweenUpdates", Constant.MINIMUM_DISTANCE_BETWEEN_UPDATES)
        val maxDistanceForSend = sharPref.getFloat("maxDistanceForSend", Constant.MAX_DISTANCE)
        val maxTimeForSend = sharPref.getInt("maxTimeForSend", Constant.MAX_TIME)
        val SAVE_FILE_DAYS = sharPref.getInt("SAVE_FILE_DAYS", Constant.SAVE_FILE_DAYS)
        val isAutoExit = sharPref.getBoolean("isAutoExit", Constant.isAutoExit)
        val timeAutoExit = sharPref.getString("timeAutoExit", Constant.timeAutoExit)
        return SettingApp(
            interval,
            fastesInterval,
            minDistanceBetweenUpdates,
            maxDistanceForSend,
            maxTimeForSend,
            SAVE_FILE_DAYS,
            isAutoExit,
            timeAutoExit!!
        )
    }

}