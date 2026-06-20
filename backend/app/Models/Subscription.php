<?php

namespace App\Models;

use App\Enums\SubscriptionStatus;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Subscription extends Model
{
    use HasUuids;

    protected $fillable = [
        'license_id',
        'product_id',
        'purchase_token',
        'google_order_id',
        'status',
        'started_at',
        'expires_at',
        'cancelled_at',
        'last_validated_at',
        'raw_google_response',
    ];

    protected function casts(): array
    {
        return [
            'status'              => SubscriptionStatus::class,
            'started_at'          => 'datetime',
            'expires_at'          => 'datetime',
            'cancelled_at'        => 'datetime',
            'last_validated_at'   => 'datetime',
            'raw_google_response' => 'array',
        ];
    }

    public function license(): BelongsTo
    {
        return $this->belongsTo(License::class);
    }

    public function isCurrentlyActive(): bool
    {
        return $this->status->isActive()
            && ($this->expires_at === null || $this->expires_at->isFuture());
    }
}
