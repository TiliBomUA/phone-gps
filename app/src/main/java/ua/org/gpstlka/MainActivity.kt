package ua.org.gpstlka

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import ua.org.gpstlka.data.Constant
import ua.org.gpstlka.data.GpsPoint
import ua.org.gpstlka.data.StatusData
import ua.org.gpstlka.helpers.GPSFileUtils
import ua.org.gpstlka.ui.home.HomeViewModel
import ua.org.gpstlka.ui.setting.SettingsViewModel
import ua.org.gpstlka.ui.setting.SettingsViewModelFactory


class MainActivity : AppCompatActivity() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private val namePreference = "def_settings"


    private val REQUEST_PERMISSION_IMEI = 109
    private val REQUEST_PERMISSION_LOCATION = 110

    private val ACTIVITY_PERMISSION_REQUEST = 111
    private val ACTIVITY_PERMISSION_REQUEST_LOCATION = 112

    //Broadcast
    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(conte: Context?, inten: Intent?) {
            val gps: GpsPoint? = inten?.getParcelableExtra(Constant.NAME_EXTRA_DATA)
            gps?.let {
                homeViewModel.setGpsData(gps)
            }

            val status: StatusData? = inten?.getParcelableExtra(Constant.NAME_EXTRA_NETSTATUS)
            status?.let {
                homeViewModel.setNetStatus(status)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val sharedPreferences = getSharedPreferences(namePreference, Context.MODE_PRIVATE)
        settingsViewModel = ViewModelProvider(this, SettingsViewModelFactory(sharedPreferences)).get(SettingsViewModel::class.java)


        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_map, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        homeViewModel.ldIsImeiEnabled.observe(this, Observer {
            val havePermision = it ?: return@Observer
            if (!havePermision) {
                buildAlertMessageNoImeiPermissions()
            } else {
                val imei = getImeiDevise()
                homeViewModel.setImei(imei)
                //Now we need test Location permissions
            }
        })

        homeViewModel.ldImei.observe(this, Observer {
            it ?: return@Observer
            if (checkPermissionsLocation()) {
                homeViewModel.isPermissionLocationEnabled(true)
            } else {
                askRequestPermissionsLocations()
            }
        })

        homeViewModel.ldPermissionLocationEnabled.observe(this, Observer {
            val havePermision = it ?: return@Observer
            if (!havePermision) {
                buildAlertMessageNoLocationPermissions()
            }
        })

        if (!checkPermissionsImei()) {
            askRequestPermissionsImei()
        } else {
            homeViewModel.isPermisionImei(true)
        }

        val filter = IntentFilter()
        filter.addAction(Constant.FILTER_NAME)
        registerReceiver(broadCastReceiver, filter)

        //Delete Old file
        GPSFileUtils.deleteOldFile(filesDir)
        homeViewModel.checkFileStatus(filesDir)
    }

    private fun getImeiDevise(): String {
        var imei = ""
        val telephonyManager = ContextCompat.getSystemService(this, TelephonyManager::class.java)
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
        } else {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    imei = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    imei = telephonyManager!!.imei
                }
                else -> {
                    imei = telephonyManager!!.getDeviceId()
                }
            }
        }
        return imei
    }

    private fun checkPermissionsImei(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun askRequestPermissionsImei() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
            showSnackbar(R.string.permission_for_imei, android.R.string.ok, View.OnClickListener { startImeiPermissionRequest() })
        } else {
            startImeiPermissionRequest()
        }
    }


    private fun startImeiPermissionRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PERMISSION_IMEI)
    }

    private fun checkPermissionsLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun askRequestPermissionsLocations() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showSnackbar(R.string.permission_for_locations, android.R.string.ok) { startLocationsPermissionRequest() }
        } else {
            startLocationsPermissionRequest()
        }
    }

    private fun startLocationsPermissionRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
    }


    private fun showSnackbar(
        snackStrId: Int,
        actionStrId: Int = 0,
        listener: View.OnClickListener? = null
    ) {
        val cons = findViewById<ConstraintLayout>(R.id.main_container)
        val snackbar = Snackbar.make(cons, getString(snackStrId), Snackbar.LENGTH_INDEFINITE)
        if (actionStrId != 0 && listener != null) {
            snackbar.setAction(getString(actionStrId), listener)
        }
        snackbar.show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_IMEI -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val imei = getImeiDevise()
                    homeViewModel.setImei(imei)
                    homeViewModel.isPermisionImei(true)
                    Toast.makeText(this, "Permission ON", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    homeViewModel.isPermisionImei(false)
                }
            }

            REQUEST_PERMISSION_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    homeViewModel.isPermissionLocationEnabled(true)
                    Toast.makeText(this, "Permission ON", Toast.LENGTH_SHORT).show()
                } else {
                    homeViewModel.isPermissionLocationEnabled(false)
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }


    private fun buildAlertMessageNoImeiPermissions() {
        showSnackbar(R.string.permission_for_imei_active, android.R.string.ok) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, ACTIVITY_PERMISSION_REQUEST)
        }
    }

    private fun buildAlertMessageNoLocationPermissions() {
        showSnackbar(R.string.permission_for_locations_active, android.R.string.ok) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, ACTIVITY_PERMISSION_REQUEST_LOCATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Log.e("onActivityResult", "req code: $requestCode, result code: $resultCode, intent $intent")
        if (requestCode == ACTIVITY_PERMISSION_REQUEST) {
            homeViewModel.isPermisionImei(checkPermissionsImei())
        }

        if (requestCode == ACTIVITY_PERMISSION_REQUEST_LOCATION) {
            homeViewModel.isPermissionLocationEnabled(checkPermissionsLocation())
        }
    }
}
