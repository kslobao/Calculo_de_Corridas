package com.calculocorridas.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.calculocorridas.domain.usecases.license.CheckLicenseUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class LicenseSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkLicenseUseCase: CheckLicenseUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        checkLicenseUseCase()
        Result.success()
    }.getOrDefault(Result.retry())

    companion object {
        const val WORK_NAME = "license_sync"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<LicenseSyncWorker>(24, TimeUnit.HOURS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
