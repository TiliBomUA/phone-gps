package ua.org.gpstlka.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GpsPoint(
    val datetime: String,
    val lat: Double,
    val long: Double,
    val speed:Float,
    val distanceAll: Float,
    val locationStatus:String?
    ) : Parcelable