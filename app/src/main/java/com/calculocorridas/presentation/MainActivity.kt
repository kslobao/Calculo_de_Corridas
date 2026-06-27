package com.calculocorridas.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.calculocorridas.data.DeviceRegistrar
import com.calculocorridas.data.ads.ConsentManager
import com.calculocorridas.data.billing.BillingManager
import com.calculocorridas.data.remoteconfig.RemoteConfigManager
import com.calculocorridas.domain.repositories.SelectorRepository
import com.calculocorridas.licensing.LicenseValidator
import com.calculocorridas.presentation.navigation.AppNavGraph
import com.calculocorridas.presentation.theme.CalculoCorridasTheme
import com.calculocorridas.selectors.SelectorConfigHolder
import com.calculocorridas.workers.LicenseSyncWorker
import com.calculocorridas.workers.SelectorSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CalcCorridas"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var licenseValidator: LicenseValidator
    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var remoteConfigManager: RemoteConfigManager
    @Inject lateinit var selectorRepository: SelectorRepository
    @Inject lateinit var selectorConfigHolder: SelectorConfigHolder
    @Inject lateinit var deviceRegistrar: DeviceRegistrar

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
            // 1. Registrar dispositivo imediatamente (não esperar o worker)
            Log.d(TAG, "Iniciando registro do dispositivo...")
            runCatching { deviceRegistrar.ensureRegistered() }
                .onSuccess { Log.i(TAG, "Dispositivo registrado/verificado com sucesso") }
                .onFailure { Log.e(TAG, "Falha no registro do dispositivo", it) }

            // 2. Carregar seletores: primeiro cache, senão busca da rede agora
            val cached = runCatching { selectorRepository.getCached() }.getOrNull()
            if (cached != null) {
                selectorConfigHolder.updateAll(cached.apps)
                Log.i(TAG, "Seletores do cache: versão ${cached.version}, apps=${cached.apps.keys}")
            } else {
                Log.w(TAG, "Cache vazio — buscando seletores da rede agora...")
                selectorRepository.getRemote()
                    .onSuccess { config ->
                        selectorConfigHolder.updateAll(config.apps)
                        runCatching { selectorRepository.saveCached(config) }
                        Log.i(TAG, "Seletores da rede: versão ${config.version}, apps=${config.apps.keys}")
                    }
                    .onFailure { Log.e(TAG, "Falha ao buscar seletores da rede", it) }
            }

            // 3. Carregar licença e remote config
            licenseValidator.loadCached()
            remoteConfigManager.fetch()
        }

        val workManager = WorkManager.getInstance(this)
        // Sync imediato de seletores (atualiza em background após o registro)
        SelectorSyncWorker.scheduleImmediate(workManager)
        SelectorSyncWorker.schedule(workManager)
        LicenseSyncWorker.schedule(workManager)

        billingManager.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}
