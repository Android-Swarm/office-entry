package com.schaeffler.officeentry.ui

import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.FragmentWifiInputBinding
import com.schaeffler.officeentry.extensions.currentSsid
import com.schaeffler.officeentry.extensions.navigate
import com.schaeffler.officeentry.extensions.textString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_wifi_input.*
import javax.inject.Inject

@AndroidEntryPoint
class WifiInputFragment :
    BindingViewModelFragment<FragmentWifiInputBinding, WifiInputFragmentViewModel>() {

    @Inject
    lateinit var wifiManager: WifiManager

    override val viewModel by viewModels<WifiInputFragmentViewModel>()

    override val layoutId = R.layout.fragment_wifi_input


    override fun onBinding(binding: FragmentWifiInputBinding) {
        super.onBinding(binding)

        binding.wifiScan = wifiManager.scanResults.map { it.SSID }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.socketError.observe(viewLifecycleOwner) { error ->
            sharedViewModel.showSnackBar(
                R.string.snack_bar_socket_error,
                error ?: getString(R.string.snack_bar_unknown_error)
            )
        }
    }

    fun onConnectButtonClicked() {
        if (ssidInput.textString.isBlank()) {
            viewModel.submitSsidError(getString(R.string.error_ssid_blank))
        } else {
            viewModel.submitCredentials(ssidInput.textString, passwordInput.textString)
        }
    }

    fun onConfirmButtonClicked(v: View) {
        if (wifiManager.currentSsid != ssidInput.textString) {
            sharedViewModel.showSnackBar(R.string.snack_bar_wrong_wifi, ssidInput.textString)

            return
        }

        val dir = WifiInputFragmentDirections.actionWifiInputFragmentToWifiConnectingFragment(
            viewModel.macAddress.value!!,
            ssidInput.textString
        )

        v.navigate(dir)
    }
}