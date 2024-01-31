// Author: Luiz Felipe Cantanhede Cristino
// Institution: GTA, COPPE, UFRJ

package com.example.wolfclient

// Necessary imports
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.Settings.Global
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.app.ActivityCompat
import com.example.wolfclient.ui.theme.WolfClientTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.opencsv.bean.CsvBindByName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    // Declaration of state variables
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val resultFromRequestState = mutableStateOf<String?>(null)
    private val latitudeState = mutableStateOf<Double?>(null)
    private val longitudeState = mutableStateOf<Double?>(null)
    private val urlState = mutableStateOf(TextFieldValue())
    private val transportation = mutableStateOf(1)
    private val timeStampValue = mutableStateOf(" ")
    private val cellIdState = mutableStateOf<Any>(0)
    private val sizeResponseState = mutableStateOf(0)
    private var toastBalloon = 0

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1000
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            val coroutineScope = rememberCoroutineScope()

            WolfClientTheme {
                val gpsMessage by resultFromRequestState
                val latitude by latitudeState
                val longitude by longitudeState
                val timestamp by timeStampValue
                val cellId by cellIdState

                val showToast = Toast.makeText(
                    this@MainActivity,
                    "Successful Request!",
                    Toast.LENGTH_SHORT
                )
                var selectedItem by remember { mutableStateOf("On foot") }
                val items = listOf(
                    "On foot",
                    "Bicycle",
                    "Motorcycle",
                    "Car",
                    "Bus",
                    "Train",
                    "VLT",
                    "Subway",
                    "Barca"
                )
                var expanded by remember { mutableStateOf(false) }
                val tipoTransporteMap = mapOf(
                    "On foot" to 1,
                    "Bicycle" to 2,
                    "Motorcycle" to 3,
                    "Car" to 4,
                    "Bus" to 5,
                    "Train" to 6,
                    "VLT" to 7,
                    "Subway" to 8,
                    "Barca" to 9
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {
                        Card(
                            modifier = Modifier.clickable { expanded = !expanded },
                            shape = RoundedCornerShape(4.dp)

                        ) {
                            Text(
                                text = "Means of transportation: $selectedItem",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        if (expanded) {

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier,
                                offset = DpOffset(
                                    120.dp,
                                    (-40).dp
                                ), // Adjust the displacement as required
                                properties = PopupProperties(focusable = true) // Adjust the properties as necessary
                            ) {
                                items.forEach { label ->
                                    DropdownMenuItem(
                                        text = { Text(text = label) },
                                        onClick = {
                                            selectedItem = label
                                            expanded =
                                                false // Closes the menu when an item is selected
                                            val valueInteger = tipoTransporteMap[selectedItem]
                                            if (valueInteger != null) {
                                                transportation.value = valueInteger
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        // Input field for the URL

                        BasicTextField(
                            value = urlState.value,
                            onValueChange = {
                                // Updates the status of the URL when the user types it in
                                urlState.value = it
                            },
                            textStyle = TextStyle(color = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(color = Color.White)
                                .border(1.dp, Color.Black)
                        )

                        // Input field for response size
                        BasicTextField(
                            value = sizeResponseState.value.toString(),
                            onValueChange = {
                                val size = it.toIntOrNull() ?: 0
                                sizeResponseState.value = size
                            },
                            textStyle = TextStyle(color = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(color = Color.White)
                                .border(1.dp, Color.Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to trigger the request
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Infinite loop to make the request every second
                                    while (true) {
                                        val url = urlState.value.text
                                        if (url.isNotEmpty()) {
                                            requestLocationAndFetchData(url)
                                        }
                                        showToast.cancel()
                                        toastBalloon = 0
                                        delay(1000) // Waits 1 second before the next request
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text("Start Continuous Request")
                        }
                        Button(
                            onClick = {
                                // Call the function to delete the CSV file
                                deleteCSVFile()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text("Delete CSV File")
                        }


                        // scrolling component to display information
                        ScrollableContent(
                            latitude,
                            longitude,
                            gpsMessage ?: "\nAwaiting response to request",
                            timestamp,
                            cellId
                        )
                        Log.d("toast", "toast: $toastBalloon")

                        if (toastBalloon == 2) {
                            showToast.show()
                            toastBalloon = 0
                        } else {
                            showToast.cancel()

                        }
                    }
                }
            }
        }
    }

    private fun deleteCSVFile() {
        val csvFileName = "clientdata.csv"

        // Directory path and CSV file
        val baseDir = File(Environment.getExternalStorageDirectory().absolutePath + "/Documents")
        val csvFile = File(baseDir, csvFileName)

        // Try deleting the CSV file
        if (csvFile.exists()) {
            val deleted = csvFile.delete()
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun getConnectedCellId(context: Context): MutableList<Any>? {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

            if (telephonyManager.phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                val cellInfoList: List<CellInfo>? = telephonyManager.allCellInfo

                if (cellInfoList != null) {
                    for (cellInfo in cellInfoList) {

                        // Checks if the cell is registered (connected)
                        if (cellInfo.isRegistered) {

                            // Obtains the Cell ID of different cell types
                            val data = mutableListOf<Any>(
                                cellInfo.cellSignalStrength.dbm,
                                cellInfo.cellSignalStrength.level
                            )
                            when (cellInfo) {
                                is CellInfoGsm -> {
                                    val cellIdentityGsm = cellInfo.cellIdentity
                                    cellIdentityGsm.mccString?.let { data.add(it) }
                                    cellIdentityGsm.mncString?.let { data.add(it) }
                                    data.add(cellIdentityGsm.cid)
                                    data.add(cellIdentityGsm.lac)
                                    data.add(5)
                                    return data
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
                                    return data
                                }

                                is CellInfoWcdma -> {
                                    val cellIdentityWcdma = cellInfo.cellIdentity
                                    cellIdentityWcdma.mccString?.let { data.add(it) }
                                    cellIdentityWcdma.mncString?.let { data.add(it) }
                                    data.add(cellIdentityWcdma.cid)
                                    data.add(cellIdentityWcdma.lac)
                                    data.add(7)
                                    return data
                                }

                                is CellInfoCdma -> {
                                    val cellIdentityCdma = cellInfo.cellIdentity
                                    data.add("")
                                    data.add("")
                                    data.add(cellIdentityCdma.basestationId)
                                    data.add("")
                                    data.add(8)
                                    return data
                                }

                                is CellInfoNr -> {
                                    val cellIdentityNr = cellInfo.cellIdentity as CellIdentityNr
                                    val cellsignalnr =
                                        cellInfo.cellSignalStrength as CellSignalStrengthNr
                                    cellIdentityNr.mccString?.let { data.add(it) }
                                    cellIdentityNr.mncString?.let { data.add(it) }
                                    data.add(cellIdentityNr.nci)
                                    data.add(cellIdentityNr.tac)
                                    data.add(9)
                                    data.add(cellsignalnr.ssRsrq)
                                    data.add(cellsignalnr.ssSinr)
                                    data.add(cellIdentityNr.nrarfcn)
                                }
                                // Add other cell types as required
                                else -> {
                                    // If you need to deal with other types of cells, do so here
                                    // Specific treatment may be required for other cell types
                                    // Depending on the case, return or continue the loop
                                }
                            }
                        }
                    }
                }
            } else {
                // If permissions have not been granted, ask the user to

                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE

                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
        return null
    }


    private fun saveDataToCSV(data: DataModel, dataCellId: MutableList<Any>) {
        val csvFileName = "clientdata.csv"

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

            if (!csvFile.exists() || csvFile.readLines().none { it.startsWith("Sequence") }) {
                val columnNames =
                    "Sequence, Transport, Timestamp, Latency, Latitude, Longitude, Signal_dbm, Signal_level, MCC, MNC, CellId, Tac/Lac, Mobile_Network, RSRQ, RSSNR, NRARFCN"
                sendDataToServer(columnNames, true)
                // Opens the writer for the CSV file
                val writer = BufferedWriter(FileWriter(csvFile, true))

                // Write the column names in the first line
                writer.write("$columnNames\n")
                writer.close()

                // If the CSV file doesn't exist, restart the count
                currentCount = 0


            }

                // Opens the writer for the CSV file


                // Gets the existing count
                val existingCount = currentCount
                // Increases the count
                val newCount = existingCount + 1
                // Updates the count in shared preferences
                sharedPreferences.edit().putInt("count", newCount).apply()

                // Checks that the mandatory fields of the DataModel are not empty and that there is CellId data
                if (isValidData(data)) {
                    // Build a formatted CSV line
                    val csvLine =
                        "$newCount, ${data.transport}, ${data.timestamp}, ${data.latency},${data.latitude},${data.longitude}," +
                                dataCellId.joinToString(",")

                    // Write the line to the file
                    Log.d("CurrentCount4", "Line: $csvLine")

                    val writer = BufferedWriter(FileWriter(csvFile, true))

                    writer.write(csvLine)
                    writer.newLine() // Adds a line break to the next entry
                    // The writer closes
                    writer.close()
                    sendDataToServer(csvLine, false) // It's not the first line
                    toastBalloon += 1
                } else {
                    Log.d("CurrentCount40000", "ERRROOOORR$data")

                }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    // Function to check if the required fields of the DataModel are filled in
    private fun isValidData(data: DataModel): Boolean {
        if (data.transport > 0 &&
            data.timestamp.isNotBlank() &&
            data.latency > 0.toLong() &&
            data.cellId != 0 &&
            data.latitude != 0.toDouble() &&
            data.longitude != 0.toDouble()

        ) {
            return true
        }
        return false
    }


    // Main function responsible for obtaining data
    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestLocationAndFetchData(url: String) {

        val permissionGranted = (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) &&

                (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000) // Intervalo de 5 segundos para as atualizações
                .setFastestInterval(1000) // Intervalo mais rápido de 1 segundo

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val lastLocation = locationResult.lastLocation
                    val latitude = lastLocation?.latitude
                    val longitude = lastLocation?.longitude
                    latitudeState.value = latitude
                    longitudeState.value = longitude
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            CoroutineScope(Dispatchers.IO).launch {

                // Aguarde até que as coordenadas de latitude e longitude estejam disponíveis
                while (latitudeState.value == null || longitudeState.value == null) {
                    delay(10000)
                }

                val sizeResponse = sizeResponseState.value
                makeRequest(url, sizeResponse, 5000) { result ->

                    val (resultRequest, latency) = result // Dismantling the Pair

                    resultRequest?.let {
                        resultFromRequestState.value = it
                    }
                    val timeStamp: String = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Date())
                    timeStamp.let {
                        timeStampValue.value = it
                    }
                    val cellId = getConnectedCellId(this@MainActivity)
                    cellId?.let {
                        cellIdState.value = it[2]
                        val dataModel = DataModel(
                            transportation.value,
                            timeStampValue.value,
                            cellIdState.value,
                            latency,
                            latitudeState.value ?: 0.0,
                            longitudeState.value ?: 0.0,
                        )
                        saveDataToCSV(dataModel, it)
                    }
                }
            }
        }
    }



    // Function that shows Hello, World on the screen
    @Composable
    fun Greeting(name: String, result: String?) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Hello, $name!",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            if (result != null) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Function that receives the data results for scrolling
    @Composable
    fun ScrollableContent(
        latitude: Double?,
        longitude: Double?,
        result: String,
        tempo: String,
        cid: Any
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                latitude?.let { lat ->
                    longitude?.let { lon ->
                        Text(
                            text = "Latitude: $lat\nLongitude: $lon\nTimestamp: $tempo\nCellId: $cid",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Greeting("world", result)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        WolfClientTheme {
            Greeting("World", "Sample Result")
        }
    }


    // Function to make an HTTP GET request
    private fun makeRequest(
        ip: String,
        sizeResponse: Int,
        port: Int,
        callback: (Pair<String?, Long>) -> Unit // Change in callback type to receive a Pair
    ) {
        val client = OkHttpClient()
        val url = HttpUrl.Builder()
            .scheme("http")
            .port(port)
            .host(ip)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val startTime = System.currentTimeMillis()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                val errorMessage = "Request failure: ${e.message}"
                Log.e("RequestFailure", errorMessage)
                // showing error using toast
                runOnUiThread {
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
                callback(Pair(null, 0)) // Returning a Pair with null values in case of failure
            }

            override fun onResponse(call: Call, response: Response) {
                val endTime = System.currentTimeMillis()
                val responseBody = response.body?.string()
                val latency = endTime - startTime
                Log.d("RequestHTTP", "Latency: $latency ms")
                toastBalloon = 1
                val result = responseBody?.let {

                    if (sizeResponse > 0 && sizeResponse < it.length) {
                        it.substring(0, sizeResponse)
                    } else {
                        it
                    }
                }
                callback(Pair(result, latency)) // Returning a Pair with the results
            }
        })
    }

    // Function for sending data to the server
    private fun sendDataToServer(csvLine: String, isFirstLine: Boolean) {

        val serverURL = HttpUrl.Builder()
            .scheme("http")
            .port(5000)
            .host(urlState.value.text)
            .build()

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("csvLine", csvLine)
            .add(
                "isFirstLine",
                isFirstLine.toString()
            ) // Adds a flag to indicate if it is the first line
            .build()

        val request = Request.Builder()
            .url(serverURL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // The server response was successful
                    // Here you can deal with the success of the update on the server
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Failed to send data to the server
            }
        })
    }


    data class DataModel(
        @CsvBindByName(column = "Transportation")
        val transport: Int,
        @CsvBindByName(column = "Timestamp")
        val timestamp: String,
        @CsvBindByName(column = "Cellid")
        val cellId: Any,
        @CsvBindByName(column = "Latência")
        val latency: Long,
        @CsvBindByName(column = "Latitude")
        val latitude: Double,
        @CsvBindByName(column = "Longitude")
        val longitude: Double
    )
}