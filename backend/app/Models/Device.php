<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Device extends Model
{
    use HasUuids;

    protected $fillable = [
        'user_id',
        'device_token',
        'package_name',
        'app_version',
        'platform',
        'ip_address',
        'is_blocked',
        'blocked_reason',
        'last_seen_at',
    ];

    protected function casts(): array
    {
        return [
            'is_blocked'   => 'boolean',
            'last_seen_at' => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function licenses(): HasMany
    {
        return $this->hasMany(License::class);
    }

    public function parserReports(): HasMany
    {
        return $this->hasMany(ParserReport::class);
    }

    public function touchSeen(string $ipAddress): void
    {
        $this->update([
            'last_seen_at' => now(),
            'ip_address'   => $ipAddress,
        ]);
    }

    public function activeLicense(): ?License
    {
        $userId = $this->user_id;

        return License::where(function ($q) use ($userId) {
                $q->where('device_id', $this->id);
                if ($userId) {
                    $q->orWhere('user_id', $userId);
                }
            })
            ->where('is_active', true)
            ->whereNull('deleted_at')
            ->where(fn($q) => $q->whereNull('expires_at')->orWhere('expires_at', '>', now()))
            ->orderByRaw("CASE source WHEN 'google' THEN 1 ELSE 0 END ASC")
            ->orderByRaw('expires_at DESC NULLS FIRST')
            ->first();
    }
}
