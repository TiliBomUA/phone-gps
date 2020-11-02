package ua.org.gpstlka.data

data class LocalTrack(
    val trackExist :Boolean,
    val list: List<LocalTrackPoint>?
)