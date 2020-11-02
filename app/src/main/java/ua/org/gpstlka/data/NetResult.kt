package ua.org.gpstlka.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class NetResult (
    val data: Int? = null,
    val Error: String? = null
) : Parcelable