package com.schaeffler.officeentry.ui

import androidx.fragment.app.viewModels
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.FragmentHomeBinding

class HomeFragment : BindingViewModelFragment<FragmentHomeBinding, HomeFragmentViewModel>() {

    override val viewModel by viewModels<HomeFragmentViewModel>()

    override val layoutId = R.layout.fragment_home
}