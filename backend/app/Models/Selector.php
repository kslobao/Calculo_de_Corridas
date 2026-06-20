<?php

namespace App\Models;

use App\Enums\AppKey;
use App\Enums\FieldType;
use App\Enums\SelectorType;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Selector extends Model
{
    use HasUuids;

    protected $fillable = [
        'version_id',
        'app_key',
        'field_type',
        'selector_type',
        'pattern_value',
        'priority',
        'is_active',
        'notes',
    ];

    protected function casts(): array
    {
        return [
            'app_key'       => AppKey::class,
            'field_type'    => FieldType::class,
            'selector_type' => SelectorType::class,
            'priority'      => 'integer',
            'is_active'     => 'boolean',
        ];
    }

    public function version(): BelongsTo
    {
        return $this->belongsTo(SelectorVersion::class, 'version_id');
    }
}
