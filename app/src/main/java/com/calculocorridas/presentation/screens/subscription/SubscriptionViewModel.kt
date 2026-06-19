package com.calculocorridas.presentation.screens.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.data.billing.BillingManager
import com.calculocorridas.data.billing.ProductIds
import com.calculocorridas.data.billing.SubscriptionState
import com.calculocorridas.domain.usecases.license.CheckLicenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val billingState: SubscriptionState = SubscriptionState.Loading,
    val selectedProductId: String = ProductIds.MONTHLY_PRO,
    val isVerifying: Boolean = false
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val checkLicenseUseCase: CheckLicenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        billingManager.state
            .onEach { state ->
                _uiState.value = _uiState.value.copy(billingState = state)
                if (state is SubscriptionState.Subscribed) {
                    verifyWithBackend(state.purchaseToken)
                }
            }
            .launchIn(viewModelScope)

        billingManager.connect()
    }

    fun selectProduct(productId: String) {
        _uiState.value = _uiState.value.copy(selectedProductId = productId)
    }

    fun purchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, _uiState.value.selectedProductId)
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
    }

    private fun verifyWithBackend(purchaseToken: String) {
        _uiState.value = _uiState.value.copy(isVerifying = true)
        viewModelScope.launch {
            checkLicenseUseCase(purchaseToken)
            _uiState.value = _uiState.value.copy(isVerifying = false)
        }
    }
}
