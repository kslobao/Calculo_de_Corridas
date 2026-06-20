<?php

namespace App\Services;

use App\Models\AppConfig;

class ConfigService
{
    public function getPublicConfig(): array
    {
        return AppConfig::publicConfig();
    }

    public function getAll(): \Illuminate\Database\Eloquent\Collection
    {
        return AppConfig::orderBy('config_key')->get();
    }

    public function update(string $key, string $value, int $adminUserId): void
    {
        AppConfig::set($key, $value, $adminUserId);
    }

    public function isMaintenanceMode(): bool
    {
        return (bool) AppConfig::get('maintenance_mode', false);
    }

    public function getMaintenanceMessage(): string
    {
        return (string) AppConfig::get('maintenance_message', 'Sistema em manutenção.');
    }
}
