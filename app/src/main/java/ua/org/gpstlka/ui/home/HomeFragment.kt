package ua.org.gpstlka.ui.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import ua.org.gpstlka.R
import ua.org.gpstlka.service.GpsService
import ua.org.gpstlka.ui.setting.SettingsViewModel

class HomeFragment : Fragment() {

    private val homeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
    }

    private val settingsViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        root.butStart.isEnabled = false
        root.butStop.isEnabled = false
        root.butSetGps.isEnabled = false
        root.text_home.text = getString(R.string.text_need_permission_imei)

        var imei = ""


        homeViewModel.ldImei.observe(viewLifecycleOwner, Observer {
            imei = it ?: return@Observer
            root.text_home.setText("imei: $imei")
            root.butStart.isEnabled = true
            root.butStop.isEnabled = true
            root.butSetGps.isEnabled = true

            if (isMyServiceRunning(GpsService::class.java)) {
                root.butStart.isEnabled = false
                root.butStop.isEnabled = true
            }
        })

        homeViewModel.ldGpsData.observe(viewLifecycleOwner, Observer {
            val gpsData = it ?: return@Observer
            tvTimeValue.text = gpsData.datetime
            tvAllDistanceValue.text = gpsData.distanceAll.toString()
            tvStatusLocationValue.text = gpsData.locationStatus
            butSetGps.isEnabled = gpsData.locationStatus != "GPS"
        })

        homeViewModel.ldStatusData.observe(viewLifecycleOwner, Observer {
            val netStatus = it ?: return@Observer
            tvDataSendValue.text = "${netStatus.colFileTOSend} дней (${netStatus.colByteToSend}b.)"
            tvDataSendTimeValue.text = netStatus.timeSend
            if (netStatus.errorSend != null) {
                tvDataSendStatusValue.text = netStatus.errorSend
            } else {
                tvDataSendStatusValue.text = getString(R.string.text_ok)
            }
        })


        root.butStart.setOnClickListener {
            //Start service
            val intent = Intent(context, GpsService::class.java)
            if (imei.isNotEmpty()) {
                intent.putExtra("imei", imei)
                intent.putExtra("appSetting", settingsViewModel.ldSettings.value)
                requireActivity().startService(intent)
                it.isEnabled = false
                root.butStop.isEnabled = true
            }
        }

        root.butStop.setOnClickListener {
            val intent = Intent(context, GpsService::class.java)
            requireActivity().stopService(intent)
            it.isEnabled = false
            root.butStart.isEnabled = true
        }

        root.butForceSend.setOnClickListener {
            homeViewModel.sendDataToServer(requireContext().filesDir)
        }

        root.butSetGps.setOnClickListener {
            buildAlertMessageNoGps()
        }

        return root
    }

    private fun <T> isMyServiceRunning(service: Class<T>): Boolean {
        val manager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runService = manager.getRunningServices(Int.MAX_VALUE)
        val isRun = runService.any { it.service.className == service.name }
        return isRun
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(getString(R.string.text_gps_wona_on))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11
                )
            }
            .setNegativeButton(getString(R.string.text_no)) { dialog, _ ->
                dialog.cancel()

            }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
}