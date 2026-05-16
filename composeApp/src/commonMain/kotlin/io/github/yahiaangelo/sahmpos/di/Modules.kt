package io.github.yahiaangelo.sahmpos.di

import io.github.yahiaangelo.sahmpos.data.local.DatabaseDriverFactory
import io.github.yahiaangelo.sahmpos.data.local.db.SahmDatabase
import io.github.yahiaangelo.sahmpos.data.remote.MockSyncServer
import io.github.yahiaangelo.sahmpos.data.remote.SyncApi
import io.github.yahiaangelo.sahmpos.data.repository.OrderRepositoryImpl
import io.github.yahiaangelo.sahmpos.data.repository.ProductRepositoryImpl
import io.github.yahiaangelo.sahmpos.data.sync.SyncManager
import io.github.yahiaangelo.sahmpos.data.sync.SyncRepositoryImpl
import io.github.yahiaangelo.sahmpos.domain.hardware.BarcodeScanner
import io.github.yahiaangelo.sahmpos.domain.hardware.PaymentTerminal
import io.github.yahiaangelo.sahmpos.domain.hardware.ReceiptPrinter
import io.github.yahiaangelo.sahmpos.domain.hardware.ReceiptRenderer
import io.github.yahiaangelo.sahmpos.domain.repository.OrderRepository
import io.github.yahiaangelo.sahmpos.domain.repository.ProductRepository
import io.github.yahiaangelo.sahmpos.domain.repository.SyncRepository
import io.github.yahiaangelo.sahmpos.domain.usecase.CheckoutOrderUseCase
import io.github.yahiaangelo.sahmpos.domain.usecase.ObserveCatalog
import io.github.yahiaangelo.sahmpos.domain.usecase.ObserveOrders
import io.github.yahiaangelo.sahmpos.domain.usecase.ScanBarcodeUseCase
import io.github.yahiaangelo.sahmpos.hardware.BarcodeScannerImpl
import io.github.yahiaangelo.sahmpos.hardware.PaymentTerminalImpl
import io.github.yahiaangelo.sahmpos.hardware.ReceiptPrinterImpl
import io.github.yahiaangelo.sahmpos.hardware.ReceiptRendererImpl
import io.github.yahiaangelo.sahmpos.presentation.orders.OrdersViewModel
import io.github.yahiaangelo.sahmpos.presentation.pos.PosViewModel
import io.github.yahiaangelo.sahmpos.presentation.sync.SyncViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val commonModule = module {
    single {
        val factory: DatabaseDriverFactory = get()
        SahmDatabase(factory.createDriver())
    }

    // Repositories
    single { ProductRepositoryImpl(get()) } bind ProductRepository::class
    single { OrderRepositoryImpl(get()) } bind OrderRepository::class

    // Remote / sync
    single { MockSyncServer() }
    single { get<MockSyncServer>().createClient() }
    single { SyncApi(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get()) }
    single { SyncManager(get()) }

    // Hardware (singletons so all consumers share state)
    single<BarcodeScanner> { BarcodeScannerImpl() }
    single<ReceiptPrinter> { ReceiptPrinterImpl() }
    single<ReceiptRenderer> { ReceiptRendererImpl() }
    single<PaymentTerminal> { PaymentTerminalImpl() }

    // Use cases
    factoryOf(::ScanBarcodeUseCase)
    factoryOf(::ObserveCatalog)
    factoryOf(::ObserveOrders)
    factoryOf(::CheckoutOrderUseCase)

    // ViewModels
    viewModelOf(::PosViewModel)
    viewModelOf(::OrdersViewModel)
    viewModelOf(::SyncViewModel)
}

fun initKoin(extra: KoinAppDeclaration? = null) {
    startKoin {
        extra?.invoke(this)
        modules(commonModule, platformModule)
    }
}