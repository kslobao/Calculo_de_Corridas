package com.calculocorridas.data.billing

sealed class SubscriptionState {
    object Loading : SubscriptionState()
    object NotSubscribed : SubscriptionState()
    data class Subscribed(val productId: String, val purchaseToken: String) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}

object ProductIds {
    const val MONTHLY_PRO = "monthly_pro"
    const val YEARLY_PRO  = "yearly_pro"
}
