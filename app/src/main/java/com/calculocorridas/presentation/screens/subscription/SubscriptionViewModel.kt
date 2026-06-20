package com.calculocorridas.presentation.screens.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.data.billing.BillingManager
import com.calculocorridas.data.billing.ProductIds
import com.calculocorridas.data.billing.SubscriptionState
import com.calculocorridas.domain.usecases.license.CheckLicenseUseCase
import com.calculocorridas.licensing.LicenseValidator
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
    val isVerifying: Boolean = false,
    val restoreResult: RestoreResult? = null
)

sealed class RestoreResult {
    object Success : RestoreResult()
    object NotFound : RestoreResult()
    object Error : RestoreResult()
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val checkLicenseUseCase: CheckLicenseUseCase,
    private val licenseValidator: LicenseValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        billingManager.state
            .onEach { state ->
                _uiState.value = _uiState.value.copy(billingState = state)
                if (state is SubscriptionState.Subscribed) {
                    validateWithBackend(state.productId, state.purchaseToken)
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

    // Triggered by user tapping "Restaurar compras"
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true, restoreResult = null)
            billingManager.queryPurchases()
            // Result arrives via billingManager.state flow → validateWithBackend
        }
    }

    fun clearRestoreResult() {
        _uiState.value = _uiState.value.copy(restoreResult = null)
    }

    // Called after Google Play confirms a purchase (new or restored).
    // Uses POST /api/v1/subscription/validate for real-time Google Play API verification.
    private fun validateWithBackend(productId: String, purchaseToken: String) {
        _uiState.value = _uiState.value.copy(isVerifying = true)
        viewModelScope.launch {
            val activated = licenseValidator.validateAndActivate(productId, purchaseToken)
            _uiState.value = _uiState.value.copy(
                isVerifying   = false,
                restoreResult = if (activated) RestoreResult.Success else RestoreResult.NotFound
            )
            // Fallback: sync via checkLicense to also pick up manual licenses
            if (!activated) {
                checkLicenseUseCase(purchaseToken)
            }
        }
    }
}
