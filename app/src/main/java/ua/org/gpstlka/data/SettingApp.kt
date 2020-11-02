package ua.org.gpstlka.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SettingApp(
     val interval: Int,
     val fastesInterval: Int,
     val minDistanceBetweenUpdates:Float,
     val maxDistanceForSend: Float,
     val maxTimeForSend: Int,
     val SAVE_FILE_DAYS: Int,
     val isAutoExit:Boolean,
     val timeAutoExit:String
) : Parcelable