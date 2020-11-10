package com.schaeffler.officeentry.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.FragmentWifiConnectingBinding
import com.schaeffler.officeentry.extensions.TAG

class WifiConnectingFragment :
    BindingViewModelFragment<FragmentWifiConnectingBinding, WifiConnectingFragmentViewModel>() {

    private val args by navArgs<WifiConnectingFragmentArgs>()

    override val viewModel by viewModels<WifiConnectingFragmentViewModel>()

    override val layoutId = R.layout.fragment_wifi_connecting

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestCameraDetails()

        viewModel.cameraDetails.observe(viewLifecycleOwner) {
            if (it == null || it.targetSsid != args.wifiSsid) {
                Log.d(TAG, "Failed! Camera details: $it")

                if (viewModel.retryCount > TRY_COUNT) {
                    viewModel.updateState(false)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        requestCameraDetails()
                    }, DELAY_PER_TRY)
                }
            } else {
                sharedViewModel.saveCameraNetworkInfo(it.macAddress, it.deviceIp)
                viewModel.updateState(true)
            }
        }
    }

    private fun requestCameraDetails() = viewModel.requestCameraDetails(args.mac)

    companion object {
        const val TRY_COUNT = 6
        const val DELAY_PER_TRY = 5000L
    }
}