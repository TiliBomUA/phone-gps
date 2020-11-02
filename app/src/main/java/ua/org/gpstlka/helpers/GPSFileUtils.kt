package ua.org.gpstlka.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.google.android.gms.maps.model.LatLng
import ua.org.gpstlka.data.Constant
import ua.org.gpstlka.data.LocalTrack
import ua.org.gpstlka.data.LocalTrackPoint
import ua.org.gpstlka.data.NetResult
import java.io.*
import java.net.ConnectException
import java.net.Socket
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


object GPSFileUtils {
    fun deleteOldFile(filesDir: File) {
        val date = Calendar.getInstance().time
        val fileFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

        val listFile = filesDir.listFiles()
        listFile?.forEach {
            val nameFile = it.name
            try {
                val parseStr = nameFile.removeSuffix("_send")
                val dateOfFile = fileFormat.parse(parseStr)  //LocalDate.parse(nameFile, file_format)
                dateOfFile?.let { dateFile ->
                    val dur = date.time - dateFile.time
                    val days = TimeUnit.MILLISECONDS.toDays(dur).toInt()
                    if (days > Constant.SAVE_FILE_DAYS) {
                        it.delete()
                    }
                }
            } catch (ex: ParseException) {

            }
        }
    }


    fun sendOneFile(file: File, imei: String): NetResult {
        val IP = Constant.SERVER_IP
        val port = Constant.SERVER_PORT

        try {
            val socket = Socket(IP, port)
            val strPresend = "TLKAAPP=$imei&"
            // write text to the socket
            val bufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            // read text from the socket
            val bufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))

            bufferedWriter.write(strPresend)
            bufferedWriter.flush()

            val resPresent = bufferedReader.readLine()
            if (resPresent != "OK") {
                socket.close()
                return NetResult(Error = "Сервер не отвечает")
            }

            val nameFile = file.name.removeSuffix("_send")

            val strSend = "date=$nameFile&\n" + file.readText()
            bufferedWriter.write(strSend)
            bufferedWriter.flush()

            val resMain = bufferedReader.readLine()
            if (resMain != "OK") {
                socket.close()
                return NetResult(Error = "Сервер не принимает даныне")
            }

            socket.close()
            file.delete()
            return NetResult(data = strSend.length)

        } catch (ex: ConnectException) {
            return NetResult(Error = ex.message)
        } catch (ex: Exception) {
            return NetResult(Error = ex.message)
        }
    }

    fun getNetWorkStatus(context: Context): Pair<Boolean, String?> {
        //Определяем наличие интернета
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        if (!isConnected) {
            return Pair(isConnected, null)
        }
        return Pair(isConnected, activeNetwork?.typeName)
    }

    fun getFileToSendInfo(filesDir: File): Pair<Int, Int> {
        val listLostToSend = filesDir.listFiles().filter { it.name.contains("_send") }
        var sizeAll = 0
        listLostToSend.forEach {
            sizeAll += it.length().toInt()
        }
        return Pair(listLostToSend.size, sizeAll)
    }

    fun loadFromFile(file: File): LocalTrack {
        val pointers = mutableListOf<LocalTrackPoint>()
        file.forEachLine {
            val listData = it.split(';')
            if (listData.size > 3) {
                pointers.add(LocalTrackPoint(listData[0], LatLng(listData[1].toDouble(), listData[2].toDouble())))
            }
        }

        if (pointers.size > 0) {
            return LocalTrack(true, pointers)
        } else {
            return LocalTrack(false, null)
        }
    }
}




