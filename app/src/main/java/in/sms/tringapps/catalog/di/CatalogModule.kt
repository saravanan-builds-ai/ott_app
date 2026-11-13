package `in`.sms.tringapps.catalog.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.sms.tringapps.catalog.data.repository.CatalogRepositoryImpl
import `in`.sms.tringapps.catalog.domain.repository.CatalogRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Binds
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository
}
