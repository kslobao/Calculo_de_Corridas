<?php

namespace App\Models;

use App\Enums\LicensePlan;
use App\Enums\LicenseSource;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\SoftDeletes;

class License extends Model
{
    use HasUuids, SoftDeletes;

    protected $fillable = [
        'user_id',
        'device_id',
        'plan',
        'source',
        'is_active',
        'expires_at',
        'purchase_token',
        'product_id',
        'reason',
        'notes',
        'created_by',
    ];

    protected function casts(): array
    {
        return [
            'plan'       => LicensePlan::class,
            'source'     => LicenseSource::class,
            'is_active'  => 'boolean',
            'expires_at' => 'datetime',
            'deleted_at' => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function device(): BelongsTo
    {
        return $this->belongsTo(Device::class);
    }

    public function createdByAdmin(): BelongsTo
    {
        return $this->belongsTo(AdminUser::class, 'created_by');
    }

    public function subscriptions(): HasMany
    {
        return $this->hasMany(Subscription::class);
    }

    public function activeSubscription(): ?Subscription
    {
        return $this->subscriptions()
            ->whereIn('status', ['active', 'grace_period'])
            ->where(fn($q) => $q->whereNull('expires_at')->orWhere('expires_at', '>', now()))
            ->latest('updated_at')
            ->first();
    }

    public function isValid(): bool
    {
        return $this->is_active
            && $this->deleted_at === null
            && ($this->expires_at === null || $this->expires_at->isFuture());
    }

    public function isPro(): bool
    {
        return $this->plan === LicensePlan::Pro && $this->isValid();
    }

    public function isManual(): bool
    {
        return $this->source->isManual();
    }
}
