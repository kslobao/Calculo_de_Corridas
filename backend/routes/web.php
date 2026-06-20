<?php

use App\Http\Controllers\Admin\AuditLogController;
use App\Http\Controllers\Admin\AuthController;
use App\Http\Controllers\Admin\ConfigController;
use App\Http\Controllers\Admin\DashboardController;
use App\Http\Controllers\Admin\DeviceController;
use App\Http\Controllers\Admin\LicenseController;
use App\Http\Controllers\Admin\ReportController;
use App\Http\Controllers\Admin\SelectorController;
use App\Http\Controllers\Admin\SubscriptionController;
use App\Http\Controllers\Admin\UserController;
use Illuminate\Support\Facades\Route;

// Auth
Route::prefix('admin')->name('admin.')->group(function () {
    Route::get('login',  [AuthController::class, 'showLogin'])->name('login');
    Route::post('login', [AuthController::class, 'login'])->middleware('throttle:10,1');
    Route::post('logout',[AuthController::class, 'logout'])->name('logout');
});

// Admin — protegido por JWT
Route::prefix('admin')->name('admin.')->middleware(['admin.jwt'])->group(function () {
    // Dashboard
    Route::get('/',           [DashboardController::class, 'index'])->name('dashboard');
    Route::get('stats',       [DashboardController::class, 'stats'])->name('stats');

    // Users
    Route::get('users',       [UserController::class, 'index'])->name('users.index');
    Route::get('users/{user}',[UserController::class, 'show'])->name('users.show');

    // Devices
    Route::get('devices',                    [DeviceController::class, 'index'])->name('devices.index');
    Route::get('devices/{device}',           [DeviceController::class, 'show'])->name('devices.show');
    Route::post('devices/{device}/block',    [DeviceController::class, 'block'])->name('devices.block');
    Route::post('devices/{device}/unblock',  [DeviceController::class, 'unblock'])->name('devices.unblock');

    // Licenses
    Route::get('licenses',                       [LicenseController::class, 'index'])->name('licenses.index');
    Route::get('licenses/create',                [LicenseController::class, 'create'])->name('licenses.create');
    Route::post('licenses',                      [LicenseController::class, 'store'])->name('licenses.store');
    Route::get('licenses/{license}',             [LicenseController::class, 'show'])->name('licenses.show');
    Route::get('licenses/{license}/edit',        [LicenseController::class, 'edit'])->name('licenses.edit');
    Route::put('licenses/{license}',             [LicenseController::class, 'update'])->name('licenses.update');
    Route::post('licenses/{license}/block',      [LicenseController::class, 'block'])->name('licenses.block');
    Route::post('licenses/{license}/unblock',    [LicenseController::class, 'unblock'])->name('licenses.unblock');

    // Subscriptions
    Route::get('subscriptions',                          [SubscriptionController::class, 'index'])->name('subscriptions.index');
    Route::get('subscriptions/{subscription}',           [SubscriptionController::class, 'show'])->name('subscriptions.show');
    Route::post('subscriptions/{subscription}/validate', [SubscriptionController::class, 'validate'])->name('subscriptions.validate');

    // Selectors
    Route::get('selectors',                    [SelectorController::class, 'index'])->name('selectors.index');
    Route::get('selectors/create',             [SelectorController::class, 'create'])->name('selectors.create');
    Route::post('selectors',                   [SelectorController::class, 'store'])->name('selectors.store');
    Route::get('selectors/{selector}/edit',    [SelectorController::class, 'edit'])->name('selectors.edit');
    Route::put('selectors/{selector}',         [SelectorController::class, 'update'])->name('selectors.update');
    Route::delete('selectors/{selector}',      [SelectorController::class, 'destroy'])->name('selectors.destroy');
    Route::post('selectors/{selector}/duplicate', [SelectorController::class, 'duplicate'])->name('selectors.duplicate');
    Route::post('selectors/publish',           [SelectorController::class, 'publish'])->name('selectors.publish');
    Route::post('selectors/rollback',          [SelectorController::class, 'rollback'])->name('selectors.rollback');

    // Config
    Route::get('config',              [ConfigController::class, 'index'])->name('config.index');
    Route::put('config/{key}',        [ConfigController::class, 'update'])->name('config.update');

    // Parser Reports
    Route::get('reports',             [ReportController::class, 'index'])->name('reports.index');
    Route::get('reports/{report}',    [ReportController::class, 'show'])->name('reports.show');

    // Audit Logs
    Route::get('logs',                [AuditLogController::class, 'index'])->name('logs.index');
});

// Redirect root to admin
Route::redirect('/', '/admin');
