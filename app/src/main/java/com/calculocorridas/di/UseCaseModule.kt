package com.calculocorridas.di

import com.calculocorridas.domain.engine.RideCalculationEngine
import com.calculocorridas.domain.engine.RuleEngine
import com.calculocorridas.domain.repositories.LicenseRepository
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.domain.repositories.RuleRepository
import com.calculocorridas.domain.repositories.SelectorRepository
import com.calculocorridas.domain.usecases.calculation.CalculateRideMetricsUseCase
import com.calculocorridas.domain.usecases.calculation.EvaluateRulesUseCase
import com.calculocorridas.domain.usecases.license.CheckLicenseUseCase
import com.calculocorridas.domain.usecases.ride.GetDashboardStatsUseCase
import com.calculocorridas.domain.usecases.ride.GetRidesUseCase
import com.calculocorridas.domain.usecases.ride.SaveRideUseCase
import com.calculocorridas.domain.usecases.selector.GetSelectorsUseCase
import com.calculocorridas.domain.usecases.selector.SyncSelectorsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides fun provideCalculateMetrics(e: RideCalculationEngine) = CalculateRideMetricsUseCase(e)
    @Provides fun provideEvaluateRules(e: RuleEngine) = EvaluateRulesUseCase(e)
    @Provides fun provideSaveRide(r: RideRepository) = SaveRideUseCase(r)
    @Provides fun provideGetRides(r: RideRepository) = GetRidesUseCase(r)
    @Provides fun provideGetDashboard(r: RideRepository) = GetDashboardStatsUseCase(r)
    @Provides fun provideGetSelectors(r: SelectorRepository) = GetSelectorsUseCase(r)
    @Provides fun provideSyncSelectors(r: SelectorRepository) = SyncSelectorsUseCase(r)
    @Provides fun provideCheckLicense(r: LicenseRepository) = CheckLicenseUseCase(r)
}
