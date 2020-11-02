package ua.org.gpstlka.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StatusData(
    val colFileTOSend:Int,
    val colByteToSend :Int,
    val timeSend : String?,
    val errorSend: String?
) : Parcelable