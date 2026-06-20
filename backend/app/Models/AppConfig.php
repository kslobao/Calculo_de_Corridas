<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Facades\Cache;

class AppConfig extends Model
{
    public $timestamps = false;

    protected $table = 'app_config';

    protected $fillable = [
        'config_key',
        'config_value',
        'value_type',
        'description',
        'is_public',
        'updated_by',
    ];

    protected function casts(): array
    {
        return [
            'is_public'  => 'boolean',
            'updated_at' => 'datetime',
        ];
    }

    public function updatedBy(): BelongsTo
    {
        return $this->belongsTo(AdminUser::class, 'updated_by');
    }

    public function typedValue(): mixed
    {
        return match($this->value_type) {
            'integer' => (int) $this->config_value,
            'float'   => (float) $this->config_value,
            'boolean' => filter_var($this->config_value, FILTER_VALIDATE_BOOLEAN),
            'json'    => json_decode($this->config_value, true),
            default   => $this->config_value,
        };
    }

    public static function get(string $key, mixed $default = null): mixed
    {
        return Cache::remember("app_config:{$key}", 300, function () use ($key, $default) {
            $config = static::where('config_key', $key)->first();
            return $config ? $config->typedValue() : $default;
        });
    }

    public static function set(string $key, string $value, int $adminUserId): void
    {
        static::where('config_key', $key)->update([
            'config_value' => $value,
            'updated_by'   => $adminUserId,
            'updated_at'   => now(),
        ]);
        Cache::forget("app_config:{$key}");
    }

    public static function publicConfig(): array
    {
        return Cache::remember('app_config:public', 300, function () {
            return static::where('is_public', true)
                ->get()
                ->mapWithKeys(fn($c) => [$c->config_key => $c->typedValue()])
                ->all();
        });
    }

    public static function clearCache(): void
    {
        Cache::forget('app_config:public');
    }
}
