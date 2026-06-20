<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class SelectorVersion extends Model
{
    public $timestamps = false;

    protected $fillable = [
        'version',
        'description',
        'is_active',
        'published_by',
        'published_at',
    ];

    protected function casts(): array
    {
        return [
            'is_active'    => 'boolean',
            'published_at' => 'datetime',
            'created_at'   => 'datetime',
        ];
    }

    protected static function boot(): void
    {
        parent::boot();
        static::creating(function ($model) {
            $model->created_at = now();
        });
    }

    public function selectors(): HasMany
    {
        return $this->hasMany(Selector::class, 'version_id');
    }

    public function publishedBy(): BelongsTo
    {
        return $this->belongsTo(AdminUser::class, 'published_by');
    }

    public static function active(): ?self
    {
        return static::where('is_active', true)->first();
    }

    public static function nextVersion(): int
    {
        return (static::max('version') ?? 0) + 1;
    }

    public function buildAndroidPayload(): array
    {
        $selectors = $this->selectors()->where('is_active', true)->get();
        $apps = [];

        foreach ($selectors->groupBy('app_key') as $appKey => $appSelectors) {
            $apps[$appKey] = [];
            foreach ($appSelectors->groupBy('field_type') as $fieldType => $patterns) {
                $androidKey = \App\Enums\FieldType::from($fieldType)->androidKey();
                $apps[$appKey][$androidKey] = $patterns->sortByDesc('priority')
                    ->map(fn($s) => [
                        'type'     => $s->selector_type,
                        'value'    => $s->pattern_value,
                        'priority' => $s->priority,
                    ])->values()->all();
            }
        }

        return [
            'version'    => $this->version,
            'updated_at' => $this->published_at?->toIso8601String() ?? now()->toIso8601String(),
            'apps'       => $apps,
        ];
    }
}
