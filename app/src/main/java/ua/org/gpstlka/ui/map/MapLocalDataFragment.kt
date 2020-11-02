package ua.org.gpstlka.ui.map


import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.fragment_map_local_data.view.*

import ua.org.gpstlka.R
import ua.org.gpstlka.data.Constant
import ua.org.gpstlka.data.LocalTrackPoint
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MapLocalDataFragment : Fragment(), OnMapReadyCallback {
    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MapViewModel::class.java)
    }

    private var curDate = Calendar.getInstance()
    private lateinit var rootView: View

    private lateinit var mMap: GoogleMap
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_map_local_data, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.MonMapLocal) as SupportMapFragment
        mapFragment.getMapAsync(this)
        rootView = root


        val sdf = SimpleDateFormat("dd MM yyyy", Locale.getDefault())

        root.tvDateOfTrack.setText(sdf.format(curDate.time))
        root.tvDateOfTrack.setOnClickListener {
            DatePickerDialog(
                requireView().context,
                android.R.style.Theme_Material_Dialog,
                { _, year, monthOfYear, dayOfMonth ->
                   changeDate(year,monthOfYear,dayOfMonth)
                },
                curDate.get(Calendar.YEAR),
                curDate.get(Calendar.MONTH),
                curDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        root.butDateNext.setOnClickListener {
            changeDate(curDate.get(Calendar.YEAR), curDate.get(Calendar.MONTH), curDate.get(Calendar.DAY_OF_MONTH)+1)
        }

        root.butDatePrev.setOnClickListener {
            changeDate(curDate.get(Calendar.YEAR), curDate.get(Calendar.MONTH), curDate.get(Calendar.DAY_OF_MONTH)-1)
        }

        root.butShowTrack.setOnClickListener {
            mapViewModel.loadTrack(curDate, requireContext().filesDir)
        }

        return root
    }

    private fun painTrack(list: List<LocalTrackPoint>?) {
        mMap.clear()
        if(list.isNullOrEmpty()){
            Toast.makeText(context,"File empty",Toast.LENGTH_SHORT).show()
            return
        }

        val options = PolylineOptions()
        options.color(Color.RED)
        options.width(5f)
        options.addAll(list.map { it.latLng })
        mMap.addPolyline(options)

        val startPoint = list.first().latLng
        mMap.addMarker(MarkerOptions().position(startPoint).title("Start"))
        val endPoint = list.last().latLng
        mMap.addMarker(MarkerOptions().position(endPoint).title("End"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(endPoint,15f))

    }


    private fun changeDate(year: Int, monthOfYear: Int, dayOfEars: Int) {
        val newDate = Calendar.getInstance().apply {set(year,monthOfYear,dayOfEars) }
        val sdf = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
        val currentDate = Calendar.getInstance()

        if (newDate.after(currentDate)) {
            Toast.makeText(requireContext(), "Дата не может быть \"Завтра\"", Toast.LENGTH_SHORT).show()
            return
        }

        val dur = currentDate.time.time - newDate.time.time
        val days = TimeUnit.MILLISECONDS.toDays(dur).toInt()
        if (days > Constant.SAVE_FILE_DAYS) {
            Toast.makeText(requireContext(), "Ранее данных мы не храним", Toast.LENGTH_SHORT).show()
            return
        }

        curDate.time = newDate.time
        rootView.tvDateOfTrack.text = sdf.format(curDate.time)

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) {
            return
        }
        mMap = googleMap
        val center = LatLng(48.938648,33.3251338)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(center))

        mapViewModel.ldTrack.observe(this, androidx.lifecycle.Observer {
            val resul = it?:return@Observer
            mMap.clear()

            if(resul.trackExist){
                painTrack(resul.list)
            }else{
                Toast.makeText(context,"No file",Toast.LENGTH_SHORT).show()
            }
        })
    }

}
