// Author: Luiz Felipe Cantanhede Cristino
// Institution: GTA, COPPE, UFRJ

package com.example.wolfserver

// Necessary imports
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.wolfserver.ui.theme.WolfServerTheme
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class MyApplication : Application() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()
        startServer()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startServer() {
        embeddedServer(Netty, port = 5000) {
            routing {
                get("/") {
                    call.respond(HttpStatusCode.OK, "Hello from the Android server!\nYou made it!")
                }
                post("/") {
                    val postParameters = call.receiveParameters()
                    Log.d("Lines", "Parâmetros: $postParameters")
                    val csvLine = postParameters["csvLine"] ?: ""

                    val firstLine = postParameters["isFirstLine"].toBoolean()
                    Log.d("Primeira Linha", "$firstLine")
                    if (csvLine.isNotEmpty()) {
                        appendToCSVFile(csvLine, firstLine)
                        call.respond(HttpStatusCode.OK, "Updated CSV file")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid data")
                    }
                }
            }
        }.start(wait = false)
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WolfServerTheme {
                // A surface container using the 'background' color from the theme
                Surface(

                    modifier = Modifier
                        .background(color = Color.White)
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(name = "")

                    Button(
                        onClick = {
                            // Call the function to delete the CSV file
                            deleteCSVFile()
                        },
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                            .padding(2.dp)
                            .absoluteOffset(x = 0.dp, y = 70.dp)
                    ) {
                        Text("Delete CSV Files")
                    }
                }
            }
        }
        // Register to receive the CsvUpdateEvent event
        EventBus.getDefault().register(this)
    }

    private fun deleteCSVFile() {
        val serverDataFileName = "serverdata.csv"
        val backupClientDataFileName = "backupclientdata.csv"

        // Directory path and CSV files
        val baseDir = File(Environment.getExternalStorageDirectory().absolutePath + "/Documents")
        val serverDataFile = File(baseDir, serverDataFileName)
        val backupClientDataFile = File(baseDir, backupClientDataFileName)

        // Try deleting the CSV file
        if (serverDataFile.exists()) {
            val deleted = serverDataFile.delete()
        }
        if (backupClientDataFile.exists()) {
            val deleted = backupClientDataFile.delete()
        }
    }

    // Method annotated with @Subscribe to receive the CsvUpdateEvent event
    @RequiresApi(Build.VERSION_CODES.R)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCsvUpdate(event: CsvUpdateEvent) {
        val showToast = Toast.makeText(
            this@MainActivity,
            "Updated CSV",
            Toast.LENGTH_SHORT
        )
        showToast.show()
        Thread.sleep(300)
        showToast.cancel()
        captureCellInfo(this@MainActivity)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister from EventBus when MainActivity is destroyed
        EventBus.getDefault().unregister(this)
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureCellInfo(context: Context) {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val timeStamp: String = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Date())


            if (telephonyManager.phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                val cellInfoList: List<CellInfo>? = telephonyManager.allCellInfo

                if (cellInfoList != null) {
                    for (cellInfo in cellInfoList) {
                        if (cellInfo.isRegistered) {
                            val data = mutableListOf<Any>(
                                timeStamp
                            )
                            val locationManager =
                                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val location: Location? =
                                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                            location?.let {
                                val latitude = it.latitude
                                val longitude = it.longitude
                                // Adding latitude and longitude
                                data.add(latitude)
                                data.add(longitude)
                            }
                            data.add(cellInfo.cellSignalStrength.dbm)
                            data.add(cellInfo.cellSignalStrength.level)

                            when (cellInfo) {
                                is CellInfoGsm -> {
                                    val cellIdentityGsm = cellInfo.cellIdentity
                                    cellIdentityGsm.mccString?.let { data.add(it) }
                                    cellIdentityGsm.mncString?.let { data.add(it) }
                                    data.add(cellIdentityGsm.cid)
                                    data.add(cellIdentityGsm.lac)
                                    data.add(5)
                                }

                                is CellInfoLte -> {
                                    val cellIdentityLte = cellInfo.cellIdentity
                                    cellIdentityLte.mccString?.let { data.add(it) }
                                    cellIdentityLte.mncString?.let { data.add(it) }
                                    data.add(cellIdentityLte.ci)
                                    data.add(cellIdentityLte.tac)
                                    data.add(6)
                                    data.add(cellInfo.cellSignalStrength.rsrq)
                                    data.add(cellInfo.cellSignalStrength.rssnr)
                                }

                                is CellInfoWcdma -> {
                                    val cellIdentityWcdma =
                                        cellInfo.cellIdentity
                                    cellIdentityWcdma.mccString?.let { data.add(it) }
                                    cellIdentityWcdma.mncString?.let { data.add(it) }
                                    data.add(cellIdentityWcdma.cid)
                                    data.add(cellIdentityWcdma.lac)
                                    data.add(7)
                                }

                                is CellInfoCdma -> {
                                    val cellIdentityCdma =
                                        cellInfo.cellIdentity
                                    data.add("")
                                    data.add("")
                                    data.add(cellIdentityCdma.basestationId)
                                    data.add("")
                                    data.add(8)
                                }

                                is CellInfoNr -> {
                                    val cellIdentityNr =
                                        cellInfo.cellIdentity as CellIdentityNr
                                    val cellSignalNr =
                                        cellInfo.cellSignalStrength as CellSignalStrengthNr
                                    cellIdentityNr.mccString?.let { data.add(it) }
                                    cellIdentityNr.mncString?.let { data.add(it) }
                                    data.add(cellIdentityNr.nci)
                                    data.add(cellIdentityNr.tac)
                                    data.add(9)
                                    data.add(cellSignalNr.ssRsrq)
                                    data.add(cellSignalNr.ssSinr)
                                    data.add(cellIdentityNr.nrarfcn)
                                }

                                else -> {
                                    // Logic for other cell types, if necessary
                                }
                            }

                            // Save to CSV file
                            saveCellInfoToCSV(data)
                            return
                        }
                    }
                }
            } else {

                ActivityCompat.requestPermissions(
                    context as ComponentActivity,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE

                )

            }
        } else {

            ActivityCompat.requestPermissions(
                context as ComponentActivity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE

            )
        }
    }

    private fun saveCellInfoToCSV(cellInfoData: List<Any>) {
        val csvFileName = "serverdata.csv"

        // CSV storage directory
        val baseDir = File(Environment.getExternalStorageDirectory().absolutePath + "/Documents")
        val csvFile = File(baseDir, csvFileName)

        try {
            // Checks if the directory exists and creates it if it doesn't
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }

            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            var currentCount = sharedPreferences.getInt("count", 0)

            if (!csvFile.exists()) {
                // Write the column names in the first line
                val writer = BufferedWriter(FileWriter(csvFile, true))
                val columnNames =
                    "Sequence, Timestamp, Latitude, Longitude, Signal_dbm, Signal_level, MCC, MNC, CellId, Tac/Lac, Mobile_Network, RSRQ, RSSNR, NRARFCN"
                writer.write("$columnNames\n")
                writer.close()

                // If the CSV file doesn't exist, restart the count
                currentCount = 0
            }

            // Opens the writer for the CSV file
            val writer = BufferedWriter(FileWriter(csvFile, true))

            // Gets the existing count
            val existingCount = currentCount
            // Increases the count
            val newCount = existingCount + 1
            // Updates the count in shared preferences
            sharedPreferences.edit().putInt("count", newCount).apply()

            val csvLine = cellInfoData.joinToString(",") { it.toString() }

            // Write the line to the file
            writer.write("$newCount,$csvLine")
            writer.newLine() // Adds a line break to the next entry

            // The writer closes
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

// Event to notify MainActivity when new data is added to CSV
class CsvUpdateEvent


fun appendToCSVFile(data: String, firstline: Boolean) {
    val csvFileName = "backupclientdata.csv"
    val baseDir = File(Environment.getExternalStorageDirectory().absolutePath + "/Documents")

    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }

    val filePath = File(baseDir, csvFileName)

    try {
        if (firstline && !filePath.exists()) {
            // Se for a primeira linha e o arquivo não existir, escreva os nomes das colunas
            val writer = BufferedWriter(FileWriter(filePath, false))
            writer.write(data)
            writer.newLine()
            writer.close()
        } else {
            // Adicione a linha de dados ao arquivo
            filePath.appendText("$data\n")
        }

        if (!firstline) {
            // Notifique o evento apenas se não for a primeira linha
            EventBus.getDefault().post(CsvUpdateEvent())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun CenteredText(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun CenteredTextPreview() {
    WolfServerTheme {
        CenteredText("Centered Text")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    CenteredText(text = "Server running!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WolfServerTheme {
        Greeting("Android")
    }
}
