package ua.org.gpstlka.ui.setting

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_settings.view.*
import ua.org.gpstlka.R
import ua.org.gpstlka.data.SettingApp
import java.text.SimpleDateFormat

class SettingsFragment : Fragment() {

    private lateinit var setting: SettingApp

    private val settingsViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        settingsViewModel.ldSettings.observe(viewLifecycleOwner, Observer {
            setting = it ?: return@Observer
            root.tvTimeIntervalValue.setText("${setting.interval}")
            root.tvTimeIntervalFastValue.setText("${setting.fastesInterval}")
            root.tvDistanceMaxIntervalValue.setText("${setting.minDistanceBetweenUpdates}")
            root.tvDataSendTimelValue.setText("${setting.maxTimeForSend}")
            root.tvDataSendDistanceValue.setText("${setting.maxDistanceForSend}")
            root.tvDataSaveDayValue.setText("${setting.SAVE_FILE_DAYS}")

            root.cbAutoExit.setOnCheckedChangeListener { buttonView, isChecked ->
                root.edAutoExitTimeValue.isEnabled = isChecked
            }

            root.cbAutoExit.isChecked = setting.isAutoExit
            root.edAutoExitTimeValue.setText(setting.timeAutoExit)
        })


        val sdf = SimpleDateFormat("HH:mm")
        val dateset = sdf.parse(settingsViewModel.ldSettings.value?.timeAutoExit!!)

        root.edAutoExitTimeValue.setOnClickListener {
            TimePickerDialog(
                requireView().context,
                { _, hour, minutes ->
                    dateset.hours = hour
                    dateset.minutes = minutes
                    root.edAutoExitTimeValue.setText(sdf.format(dateset))
                },
                dateset.hours,
                dateset.minutes,
                true
            ).show()
        }

        root.butSettingsSave.setOnClickListener {
            val timeInterval = EditUtils.getEditableValue(root.tvTimeIntervalValue.text.toString(), setting.interval)
            val timeIntervalFast = EditUtils.getEditableValue(root.tvTimeIntervalFastValue.text.toString(), setting.fastesInterval)
            val distanceMaxInterval = EditUtils.getEditableValue(root.tvDistanceMaxIntervalValue.text.toString(), setting.minDistanceBetweenUpdates)
            val dataSendTimeValue = EditUtils.getEditableValue(root.tvDataSendTimelValue.text.toString(), setting.maxTimeForSend)
            val dataSendDistance = EditUtils.getEditableValue(root.tvDataSendDistanceValue.text.toString(), setting.maxDistanceForSend)
            val isAutoExit = root.cbAutoExit.isChecked
            val timeAutoEXit = EditUtils.getEditableValue(root.edAutoExitTimeValue.text.toString(), setting.timeAutoExit)

            settingsViewModel.setSettings(
                timeInterval,
                timeIntervalFast,
                distanceMaxInterval,
                dataSendTimeValue,
                dataSendDistance,
                isAutoExit,
                timeAutoEXit
            )
        }

        return root
    }


    object EditUtils {

        @Suppress("UNCHECKED_CAST")
        fun <T> getEditableValue(newValue: String, oldValue: T): T? {

            if (newValue.isEmpty()) {
                return null
            }

            if (newValue == oldValue.toString()) {
                return null
            }

            when (oldValue) {
                is Int -> return newValue.toInt() as T
                is String -> return newValue as T
                is Float -> return newValue.toFloat() as T
            }
            return null
        }
    }

}