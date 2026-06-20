<?php

namespace App\Providers;

use App\Repositories\Contracts\DeviceRepositoryInterface;
use App\Repositories\Contracts\LicenseRepositoryInterface;
use App\Repositories\Contracts\SelectorRepositoryInterface;
use App\Repositories\DeviceRepository;
use App\Repositories\LicenseRepository;
use App\Repositories\SelectorRepository;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->bind(DeviceRepositoryInterface::class, DeviceRepository::class);
        $this->app->bind(LicenseRepositoryInterface::class, LicenseRepository::class);
        $this->app->bind(SelectorRepositoryInterface::class, SelectorRepository::class);
    }

    public function boot(): void
    {
        //
    }
}
