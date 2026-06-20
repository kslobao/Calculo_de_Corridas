<?php

namespace App\Repositories;

use App\Models\Selector;
use App\Models\SelectorVersion;
use App\Repositories\Contracts\SelectorRepositoryInterface;
use Illuminate\Support\Facades\DB;

class SelectorRepository implements SelectorRepositoryInterface
{
    public function activeVersion(): ?SelectorVersion
    {
        return SelectorVersion::where('is_active', true)->first();
    }

    public function versionByNumber(int $version): ?SelectorVersion
    {
        return SelectorVersion::where('version', $version)->first();
    }

    public function publishNewVersion(string $description, int $adminUserId): SelectorVersion
    {
        return DB::transaction(function () use ($description, $adminUserId) {
            $currentActive = $this->activeVersion();
            $nextVersion   = SelectorVersion::nextVersion();

            $newVersion = SelectorVersion::create([
                'version'      => $nextVersion,
                'description'  => $description,
                'is_active'    => false,
                'published_by' => $adminUserId,
                'published_at' => now(),
            ]);

            if ($currentActive) {
                $currentActive->selectors()
                    ->where('is_active', true)
                    ->each(function (Selector $s) use ($newVersion) {
                        Selector::create([
                            'version_id'    => $newVersion->id,
                            'app_key'       => $s->app_key->value,
                            'field_type'    => $s->field_type->value,
                            'selector_type' => $s->selector_type->value,
                            'pattern_value' => $s->pattern_value,
                            'priority'      => $s->priority,
                            'is_active'     => true,
                            'notes'         => $s->notes,
                        ]);
                    });

                $currentActive->update(['is_active' => false]);
            }

            $newVersion->update(['is_active' => true]);

            return $newVersion->fresh();
        });
    }

    public function rollbackToPrevious(): ?SelectorVersion
    {
        return DB::transaction(function () {
            $current  = $this->activeVersion();
            $previous = SelectorVersion::where('is_active', false)
                ->orderByDesc('version')
                ->first();

            if (!$previous) {
                return null;
            }

            $current?->update(['is_active' => false]);
            $previous->update(['is_active' => true]);

            return $previous->fresh();
        });
    }
}
