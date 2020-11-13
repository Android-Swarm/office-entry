package com.schaeffler.officeentry.ui

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.ogawa.temiirsdk.IrDataUtil
import com.robotemi.sdk.TtsRequest
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.extensions.*
import com.schaeffler.officeentry.preference.PreferenceRepository
import com.schaeffler.officeentry.thermal.*
import com.schaeffler.officeentry.utils.AppState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@OptIn(FlowPreview::class)
class MainActivityViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PreferenceRepository,
) : BaseViewModel() {
    private val _snackBarFlow = MutableSharedFlow<Pair<String, Int>>()
    val snackBarFlow: SharedFlow<Pair<String, Int>> = _snackBarFlow

    private val _ttsRequestFlow = MutableSharedFlow<Pair<String, Boolean>>()
    val ttsRequestFlow = _ttsRequestFlow.map { (message, show) ->
        TtsRequest.create(message, show)
    }

    private val _maskDetected = MutableStateFlow(true)
    val maskDetected = _maskDetected.debounce(1000).alsoDo {
        if (!it) {
            Log.d(TAG, "User not wearing mask!")
            requestTemiSpeak(R.string.tts_no_mask_detected)
        } else {
            Log.d(TAG, "User is wearing mask")
        }
    }.asLiveData()

    private val _cameraDetailsState = MutableStateFlow<CameraDetails?>(null)
    val cameraDetails = _cameraDetailsState.asLiveData()

    private val _batteryString = MutableStateFlow("")
    val batteryString = _batteryString.asLiveData()

    private val _temiIrSdk = MutableStateFlow(false)

    /** Whether the thermal camera socket is connected and the SDK is ready. */
    val isThermalCameraReady = _temiIrSdk.combine(_batteryString) { sdk, battery ->
        sdk && battery.isNotBlank()
    }.asLiveData()

    /** Contains camera's mac, ip, and the sdk state. Both Mac and IP are not blank. */
    private val cameraConnectItems = repository.cameraConnectionDetail
        .filter { (mac, ip) -> mac.isNotBlank() && ip.isNotBlank() }
        .combine(_temiIrSdk) { (mac, ip), sdkState -> Triple(mac, ip, sdkState) }
        .debounce(2000)

    /** Flow for FRM frame 1. */
    private val rawFrmOne = MutableSharedFlow<TmIrResponse.FRM>()

    /** Flow for FRM frame 0. */
    private val rawFrmZero = MutableSharedFlow<TmIrResponse.FRM>()

    /** Flow of pairs of FRM frame 1 and 0. */
    private val frmFlow = rawFrmOne
        .zip(rawFrmZero) { frmOne, frmZero -> Pair(frmZero, frmOne) }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Thermal image from the thermal camera. */
    val thermalImage = frmFlow.map { (frmZero, frmOne) ->
        val rawTempValues = IrDataUtil.getTemperatureResult(frmZero.data, frmOne.data)!!

        val interpolated =
            IrDataUtil.setInterpolationAlgorithm(rawTempValues, CameraConfig.INTERPOLATION)!!

        val colorList =
            IrDataUtil.getColorList(rawTempValues, interpolated, CameraConfig.INTERPOLATION)!!

        // Remove gray color and convert to color integers
        colorList.map { Color.rgb(it[1], it[2], it[3]) }.toTypedArray()
    }.catch { intArrayOf() }
        .filter { it.isNotEmpty() }
        .map { colors -> colors.toBitmap(CameraConfig.IMAGE_WIDTH, CameraConfig.IMAGE_HEIGHT) }
        .flowOn(Dispatchers.Default)
        .asLiveData()

    /** Measured temperature and distance from the camera. */
    val temperatureFlow = frmFlow
        .map { (frmZero, frmOne) ->
            val distance = (frmZero.distance + frmOne.distance) / 2
            val temp = IrDataUtil.getResult(frmZero.body, frmOne.body) ?: -1.0

            Log.d(TAG, "Temp: $temp, Distance: $distance")

            Pair(temp, distance)
        }.filter { (temp, distance) -> temp in 30.0..42.0 && distance in Int.MIN_VALUE until 800 }
        .map { (temp, _) -> temp.toFloat() }

    /** Temperature blasted from the camera. */
    val temperature = temperatureFlow.asLiveData()

    private val _finalTemperature = MutableSharedFlow<Float>()

    /** The average temperature of a number of temperatures. The user's final temperature. */
    val finalTemperature = _finalTemperature.asLiveData()

    val completionMessage = _finalTemperature.map {
        if (it > 37.4f) {
            requestTemiSpeak(R.string.tts_abnormal_temp, it)
            getString(R.string.label_abnormal_temp)
        } else {
            requestTemiSpeak(R.string.tts_normal_temp, it)
            getString(R.string.label_normal_temp)
        }
    }.asLiveData()

    private val _userInteraction = MutableStateFlow(false)
    val userInteraction: StateFlow<Boolean> = _userInteraction

    private val _appState = MutableStateFlow(AppState.IDLE)
    val appState: StateFlow<AppState> = _appState
    val appStateLive = _appState.asLiveData()

    private val _receivedUserTemperature = MutableStateFlow(false)
    val tempDetecting =
        _receivedUserTemperature.combine(appState) { first, state -> first to state }
            .distinctUntilChanged()

    /** `true` when the user has recorded at least 1 valid temperature. */
    val receivedUserTemperature = _receivedUserTemperature.asLiveData()

    init {
        // Get camera details
        viewModelScope.launch {
            cameraConnectItems.map { (mac, _, _) -> mac.also { Log.d(TAG, "Mac: $mac") } }
                .distinctUntilChanged()
                .collectLatest {
                    _cameraDetailsState emitValue CameraDetailsFetcher(it)
                        .cameraDetailsAsync(this)
                        .await()
                }
        }

        // Poll for battery
        viewModelScope.launch {
            cameraConnectItems.collectLatest { (mac, ip, _) ->
                pollBattery(ip, mac)
            }
        }

        // Start temperature taking
        viewModelScope.launch {
            cameraConnectItems.filter { (_, _, sdkReady) -> sdkReady }
                .collectLatest { (mac, ip, _) ->
                    startTemperatureTaking(ip, mac)
                }
        }
    }

    fun showSnackBar(stringId: Int, length: Int = Snackbar.LENGTH_LONG) =
        _snackBarFlow emitValue (getString(stringId) to length)

    fun showSnackBar(stringId: Int, vararg args: Any?, length: Int = Snackbar.LENGTH_LONG) =
        _snackBarFlow emitValue (getString(stringId).format(*args) to length)

    fun requestTemiSpeak(stringId: Int, vararg args: Any?, show: Boolean = false) =
        _ttsRequestFlow emitValue (getString(stringId).format(*args) to show)

    fun setMaskDetected(detected: Boolean) {
        _maskDetected.value = detected
    }

    fun setTemiIrSdkState(ready: Boolean) {
        _temiIrSdk.value = ready
    }

    fun saveCameraNetworkInfo(mac: String, ip: String) = viewModelScope.launch {
        repository.saveCameraConnectionDetails(mac, ip)
    }

    fun updateUserInteraction(interacting: Boolean) {
        _userInteraction.value = interacting
    }

    fun updateApplicationState(state: AppState) {
        _appState.value = state
    }

    fun finalizeTemperature(temperature: Float) = _finalTemperature emitValue temperature

    fun updateReceivedFirstTemperature(received: Boolean) {
        _receivedUserTemperature.value = received
    }

    private suspend fun pollBattery(ip: String, mac: String) {
        withContext(Dispatchers.IO) {
            try {
                withSocketOperation(ip, CameraConfig.BATTERY_SOCKET) { _, writer, reader ->
                    Log.d(TAG, "Connected to battery socket at $ip")

                    while (true) {
                        // Check if coroutine scope is cancelled
                        currentCoroutineContext().ensureActive()

                        // Send battery command
                        writer writeString CameraConfig.BATTERY_COMMAND
                        Log.d(TAG, "Sent battery request command")

                        // Read battery response
                        val response =
                            (reader readStringLength CameraConfig.MAX_BATTERY_RESPONSE_LENGTH)
                                .also {
                                    Log.d(TAG, "Response: $it")
                                }

                        // Update live data
                        response.filter { it.isDigit() }
                            .toFloatOrNull()
                            ?.let { number ->
                                val battery = ((number - 800f) / 200f).coerceIn(0f, 1f)

                                _batteryString.value = "%.1f".format(battery * 100) + "%"
                            }

                        delay(10000)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Encountered exception at battery socket", e)

                _batteryString.value = ""

                reFetchCameraDetails(mac)
            }
        }
    }

    private suspend fun startTemperatureTaking(ip: String, mac: String) =
        withContext(Dispatchers.IO) {
            try {
                withSocketOperation(ip, CameraConfig.TEMP_PORT) { _, tempWriter, tempReader ->
                    Log.d(TAG, "Connected to temp socket at $ip")

                    do {
                        val eem = readThermalData<TmIrResponse.EEM>(tempReader, tempWriter) {
                            it.writeString(CameraConfig.EEM_COMMAND) // Need to issue the command to get EEM

                            Log.d(TAG, "Sent first temperature read command")
                        }

                        val correction = eem.correction

                        if (correction != 0) {
                            Log.d(TAG, "Correction failed! Correction: $correction")
                        } else {
                            Log.d(TAG, "Successfully corrected temperature measurement!")
                        }

                    } while (correction != 0)

                    // Fetch FRM pairs
                    while (true) {
                        // Cancel if the coroutine scope has been cancelled
                        currentCoroutineContext().ensureActive()

                        val frmArray = mutableMapOf<Int, TmIrResponse.FRM>()

                        // FRM 1
                        var frmOne: TmIrResponse.FRM

                        do {
                            frmOne = readThermalData(tempReader, tempWriter)
                        } while (
                            frmOne.data.isEmpty() ||
                            frmOne.frameNo !in listOf(0, 1) ||
                            frmOne.distance == 0
                        )

                        frmArray[frmOne.frameNo] = frmOne

                        Log.d(TAG, "FRM 1 -> frame: ${frmOne.frameNo} distance: ${frmOne.distance}")

                        // FRM 2
                        var frmTwo: TmIrResponse.FRM

                        do {
                            frmTwo = readThermalData(tempReader, tempWriter)
                        } while (
                            frmTwo.data.isEmpty() ||
                            frmTwo.frameNo !in listOf(0, 1) ||
                            frmTwo.distance == 0 ||
                            frmTwo.frameNo == frmOne.frameNo
                        )

                        frmArray[frmTwo.frameNo] = frmTwo

                        Log.d(TAG, "FRM 2 -> frame: ${frmTwo.frameNo} distance: ${frmTwo.distance}")

                        rawFrmZero emitValue frmArray.getValue(0)
                        rawFrmOne emitValue frmArray.getValue(1)
                    }
                }
            } catch (e: IOException) {
                reFetchCameraDetails(mac)
                // This should re-trigger the temperature flow

                return@withContext
            } catch (e: CancellationException) {
                Log.d(TAG, "Stopping due to coroutine cancellation")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Wrong starting point!")

                rePostSdkState()
            } catch (e: Exception) {
                Log.e(TAG, "Restarting socket! Encountered exception ", e)

                rePostSdkState()
            }
        }

    /**
     * Requests for either an EEM data or FRM data. The method will keep dropping frames that are not
     * of the requested type.
     *
     * @param T The type of data to be retrieved.
     * @param request A lambda function for sending commands to the socket. Make sure to use this
     *              parameter when requesting for an EEM data.
     *
     * @return An EEM or FRM data.
     */
    private inline fun <reified T : TmIrResponse> readThermalData(
        reader: InputStream,
        writer: OutputStream,
        request: (OutputStream) -> Unit = {}
    ): T {
        var result: TmIrResponse?

        do {
            request(writer)

            result = readSocketForTemp(reader)
                ?.also { response ->
                    Log.d(
                        TAG,
                        "${response::class.simpleName} received. Length: ${response.body.length}"
                    )
                }
        } while (result !is T)

        return result
    }

    /**
     * Reads temperature data from the socket's [InputStream]. The data can be of 2 type: EEM or FRM.
     * Each can be uniquely identified by the 7 bytes header.
     *
     * @param reader The temperature socket input stream.
     *
     * @return EEM or FRM data.
     *
     * @throws IllegalStateException If the starting header is not EEM's or FRM's. **Do note that
     * this happens pretty often.**
     */
    private fun readSocketForTemp(reader: InputStream): TmIrResponse? {
        // Read header
        val header = reader readBytesLength 7

        return when {
            header.contentEquals(byteArrayOf(0x4d, 0x4c, 0x58, 0x5f, 0x46, 0x52, 0x4d)) -> {
                // FRM data
                val data = reader readBytesLength 1741
                TmIrResponse.FRM(data.toHexString())
            }

            header.contentEquals(byteArrayOf(0x4d, 0x4c, 0x58, 0x5f, 0x45, 0x45, 0x4d)) -> {
                // EEM data
                val data = reader readBytesLength 1666
                TmIrResponse.EEM(data.toHexString())
            }

            else -> {
                Log.d(TAG, "Discarding wrong starting point! Header=${header.toHexString()}")

                reader readStringLength 1741  // Discard data
                null
            }
        }
    }

    /**
     * Calls the thermal camera's backend API to get the camera details. Also updates the
     * proto data store's camera ip and camera mac.
     *
     * @param mac The camera's MAC address. This should not be blank
     */
    private fun reFetchCameraDetails(mac: String) = viewModelScope.launch {
        updateApplicationState(AppState.IDLE)

        Log.d(TAG, "Re-fetching camera details in 5 seconds")
        delay(5000)

        try {
            val newDetails = CameraDetailsFetcher(mac).cameraDetailsAsync(this).await()!!
            repository.saveCameraConnectionDetails(newDetails.macAddress, newDetails.deviceIp)

            Log.d(TAG, "Fetched new camera details, overwritten data store")
        } catch (e: Exception) {
            Log.e(TAG, "Unable to re-fetch camera details: ", e)
        }

        rePostSdkState()
    }

    private fun rePostSdkState() {
        val tmp = _temiIrSdk.value

        _temiIrSdk.value = (!tmp).also { Log.d(TAG, "Set SDK ready to $it") }
        _temiIrSdk.value = tmp.also { Log.d(TAG, "Set SDK ready to $it") }
    }

    private fun getString(id: Int) = context.getString(id)
}