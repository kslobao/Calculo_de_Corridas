package com.calculocorridas.di

import com.calculocorridas.data.repositories.LicenseRepositoryImpl
import com.calculocorridas.data.repositories.RideRepositoryImpl
import com.calculocorridas.data.repositories.RuleRepositoryImpl
import com.calculocorridas.data.repositories.SelectorRepositoryImpl
import com.calculocorridas.domain.repositories.LicenseRepository
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.domain.repositories.RuleRepository
import com.calculocorridas.domain.repositories.SelectorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindRideRepository(impl: RideRepositoryImpl): RideRepository

    @Binds @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds @Singleton
    abstract fun bindSelectorRepository(impl: SelectorRepositoryImpl): SelectorRepository

    @Binds @Singleton
    abstract fun bindLicenseRepository(impl: LicenseRepositoryImpl): LicenseRepository
}
