package ua.org.gpstlka.data

object Constant {

    const val SERVER_IP = "127.0.0.1"
    const val SERVER_PORT = 21111

    const val NAME_EXTRA_NETSTATUS = "tlka_netstatus"
    const val NAME_EXTRA_DATA  = "tlka_data"
    const val FILTER_NAME ="ua.org.gpstlka"

    const val INTERVAL: Int = 10
    const val FASTEST_INTERVAL: Int = 5
    const val MINIMUM_DISTANCE_BETWEEN_UPDATES = 3f

    const val MAX_DISTANCE: Float = 500f
    const val MAX_TIME: Int = 10
    const val SAVE_FILE_DAYS :Int = 30

    const val isAutoExit: Boolean = false
    const val  timeAutoExit: String = "17:00"
}