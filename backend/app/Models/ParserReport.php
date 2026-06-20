<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ParserReport extends Model
{
    use HasUuids;

    public $timestamps = false;

    protected $fillable = [
        'device_id',
        'app_key',
        'selector_version',
        'raw_texts',
        'parsed_value',
        'parsed_distance',
        'parsed_duration_min',
        'success',
        'error_message',
        'app_version',
    ];

    protected function casts(): array
    {
        return [
            'raw_texts'      => 'array',
            'parsed_value'   => 'float',
            'parsed_distance'=> 'float',
            'success'        => 'boolean',
            'created_at'     => 'datetime',
        ];
    }

    protected static function boot(): void
    {
        parent::boot();
        static::creating(function ($model) {
            $model->created_at = now();
        });
    }

    public function device(): BelongsTo
    {
        return $this->belongsTo(Device::class);
    }
}
