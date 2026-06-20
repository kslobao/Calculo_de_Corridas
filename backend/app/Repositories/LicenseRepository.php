<?php

namespace App\Repositories;

use App\Models\Device;
use App\Models\License;
use App\Repositories\Contracts\LicenseRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

class LicenseRepository implements LicenseRepositoryInterface
{
    public function findForDevice(Device $device): ?License
    {
        return $this->baseQuery($device)
            ->orderByRaw("CASE source WHEN 'google' THEN 1 ELSE 0 END ASC")
            ->orderByRaw('expires_at DESC NULLS FIRST')
            ->first();
    }

    public function findManualForDevice(Device $device): ?License
    {
        return $this->baseQuery($device)
            ->whereNotIn('source', ['free', 'google'])
            ->orderByRaw('expires_at DESC NULLS FIRST')
            ->first();
    }

    public function findGoogleForDevice(Device $device): ?License
    {
        return $this->baseQuery($device)
            ->where('source', 'google')
            ->first();
    }

    public function create(array $data): License
    {
        return License::create($data);
    }

    public function update(License $license, array $data): License
    {
        $license->update($data);
        return $license->fresh();
    }

    public function paginate(int $perPage = 20, array $filters = []): LengthAwarePaginator
    {
        $query = License::with(['device', 'user', 'createdByAdmin'])
            ->withTrashed()
            ->latest('updated_at');

        if (!empty($filters['plan'])) {
            $query->where('plan', $filters['plan']);
        }

        if (!empty($filters['source'])) {
            $query->where('source', $filters['source']);
        }

        if (isset($filters['is_active'])) {
            $query->where('is_active', (bool) $filters['is_active']);
        }

        if (!empty($filters['search'])) {
            $search = $filters['search'];
            $query->where(function ($q) use ($search) {
                $q->where('purchase_token', 'ilike', "%{$search}%")
                  ->orWhere('product_id', 'ilike', "%{$search}%")
                  ->orWhere('reason', 'ilike', "%{$search}%");
            });
        }

        return $query->paginate($perPage);
    }

    private function baseQuery(Device $device): \Illuminate\Database\Eloquent\Builder
    {
        $userId = $device->user_id;

        return License::where(function ($q) use ($device, $userId) {
                $q->where('device_id', $device->id);
                if ($userId) {
                    $q->orWhere('user_id', $userId);
                }
            })
            ->where('is_active', true)
            ->whereNull('deleted_at')
            ->where(fn($q) => $q->whereNull('expires_at')->orWhere('expires_at', '>', now()));
    }
}
