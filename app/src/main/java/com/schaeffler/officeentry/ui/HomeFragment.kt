package com.schaeffler.officeentry.ui

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.viewModels
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.FragmentHomeBinding
import com.schaeffler.officeentry.extensions.toQrCode
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : BindingViewModelFragment<FragmentHomeBinding, HomeFragmentViewModel>() {

    override val viewModel by viewModels<HomeFragmentViewModel>()

    override val layoutId = R.layout.fragment_home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val barcodeTreeObserver = imageQrCode.viewTreeObserver
        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                imageQrCode?.apply {
                    val qrCode = SCHAEFFLER_SAFE_ENTRY_URL.toQrCode(width, height)
                    setImageBitmap(qrCode)
                }

                if (barcodeTreeObserver.isAlive) {
                    barcodeTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        }

        barcodeTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    companion object {
        const val SCHAEFFLER_SAFE_ENTRY_URL =
            "https://temperaturepass.ndi-api.gov.sg/login/PROD-199604030H-125413-SCHAEFFLERSINGAPOREPTELTD-SE"
    }
}