package com.calculocorridas.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.calculocorridas.data.ads.ConsentManager
import com.calculocorridas.data.billing.BillingManager
import com.calculocorridas.data.remoteconfig.RemoteConfigManager
import com.calculocorridas.licensing.LicenseValidator
import com.calculocorridas.presentation.navigation.AppNavGraph
import com.calculocorridas.presentation.theme.CalculoCorridasTheme
import com.calculocorridas.workers.LicenseSyncWorker
import com.calculocorridas.workers.SelectorSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var licenseValidator: LicenseValidator
    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var remoteConfigManager: RemoteConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        initializeApp()

        setContent {
            CalculoCorridasTheme {
                AppNavGraph()
            }
        }
    }

    private fun initializeApp() {
        consentManager.requestConsent(this)

        lifecycleScope.launch {
            licenseValidator.loadCached()
            remoteConfigManager.fetch()
        }

        val workManager = WorkManager.getInstance(this)
        SelectorSyncWorker.schedule(workManager)
        LicenseSyncWorker.schedule(workManager)

        billingManager.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}
