package com.schaeffler.officeentry.ui

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel

abstract class BindingViewModelFragment<T : ViewDataBinding, VM : ViewModel> :
    BindingFragment<T>() {

    protected abstract val viewModel: VM

    final override fun onBindingReflection(binding: T) {
        super.onBindingReflection(binding)

        // Bind View Model
        tryAssignBinding("setViewModel", viewModel::class.java, binding, viewModel)
    }
}