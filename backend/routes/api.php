<?php

use App\Http\Controllers\Api\V1\ConfigController;
use App\Http\Controllers\Api\V1\DeviceController;
use App\Http\Controllers\Api\V1\GoogleRtdnController;
use App\Http\Controllers\Api\V1\LicenseController;
use App\Http\Controllers\Api\V1\ParserReportController;
use App\Http\Controllers\Api\V1\SelectorController;
use App\Http\Controllers\Api\V1\SubscriptionController;
use App\Http\Controllers\HealthController;
use Illuminate\Support\Facades\Route;

// Health endpoints — sem autenticação, sem rate limit de API
Route::prefix('health')->group(function () {
    Route::get('/',          [HealthController::class, 'index']);
    Route::get('/database',  [HealthController::class, 'database']);
    Route::get('/redis',     [HealthController::class, 'redis']);
    Route::get('/queue',     [HealthController::class, 'queue']);
});

// Google RTDN Webhook — sem device auth, validação via token de query
Route::post('v1/google/rtdn', [GoogleRtdnController::class, 'handle'])
    ->middleware('throttle:120,1');

// Registro de device — sem device auth (é o bootstrap)
Route::post('v1/device/register', [DeviceController::class, 'register'])
    ->middleware('throttle:10,1');

// Endpoints que requerem device token válido
Route::middleware(['device.auth', 'maintenance', 'throttle:60,1'])->prefix('v1')->group(function () {
    Route::get('selectors',              [SelectorController::class, 'index']);
    Route::post('license/check',         [LicenseController::class, 'check']);
    Route::post('subscription/validate', [SubscriptionController::class, 'validate']);
    Route::post('parser/report',         [ParserReportController::class, 'store']);
    Route::get('config',                 [ConfigController::class, 'index']);
});
