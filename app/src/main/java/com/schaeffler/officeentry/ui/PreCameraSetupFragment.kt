package com.schaeffler.officeentry.ui

import android.Manifest
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.FragmentPreCameraSetupBinding
import com.schaeffler.officeentry.extensions.TAG
import com.schaeffler.officeentry.extensions.currentSsid
import com.schaeffler.officeentry.extensions.navigate
import com.schaeffler.officeentry.thermal.CameraConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PreCameraSetupFragment : BindingFragment<FragmentPreCameraSetupBinding>() {

    @Inject
    lateinit var wifiManager: WifiManager

    override val layoutId = R.layout.fragment_pre_camera_setup

    /** For requesting location permission. If the permission is not granted, return to the previous screen. */
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                sharedViewModel.showSnackBar(R.string.snack_bar_location_rejected)
                requireActivity().onBackPressed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission
        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun onConfirmButtonClicked(v: View) {
        if (wifiManager.currentSsid != CameraConfig.OGAWA_HOTSPOT_NAME) {
            sharedViewModel.showSnackBar(R.string.snack_bar_not_connected_to_ogawa)
            Log.d(TAG, "User is connected to ${wifiManager.currentSsid}")
            return
        }

        v.navigate(R.id.action_preCameraSetupFragment_to_wifiInputFragment)
    }

}