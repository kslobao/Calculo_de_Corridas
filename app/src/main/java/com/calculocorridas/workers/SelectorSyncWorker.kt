package com.calculocorridas.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.calculocorridas.data.analytics.AnalyticsManager
import com.calculocorridas.domain.repositories.SelectorRepository
import com.calculocorridas.selectors.SelectorConfigHolder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SelectorSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val selectorRepository: SelectorRepository,
    private val selectorConfigHolder: SelectorConfigHolder,
    private val analyticsManager: AnalyticsManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = selectorRepository.getRemote()
        return if (result.isSuccess) {
            val config = result.getOrThrow()
            selectorRepository.saveCached(config)
            selectorConfigHolder.updateAll(config.apps)
            analyticsManager.logSelectorSync(config.version, true)
            Result.success()
        } else {
            analyticsManager.logSelectorSync(0, false)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "selector_sync"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SelectorSyncWorker>(6, TimeUnit.HOURS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediate(workManager: WorkManager) {
            workManager.enqueue(OneTimeWorkRequestBuilder<SelectorSyncWorker>().build())
        }
    }
}
