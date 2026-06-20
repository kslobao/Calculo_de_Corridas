<?php

namespace App\Repositories;

use App\Models\Device;
use App\Repositories\Contracts\DeviceRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

class DeviceRepository implements DeviceRepositoryInterface
{
    public function findByToken(string $token): ?Device
    {
        return Device::where('device_token', $token)->first();
    }

    public function createOrUpdate(array $data): Device
    {
        return Device::updateOrCreate(
            ['device_token' => $data['device_token']],
            [
                'package_name' => $data['package_name'],
                'app_version'  => $data['app_version'] ?? null,
                'platform'     => $data['platform'] ?? 'android',
                'last_seen_at' => now(),
            ]
        );
    }

    public function touchSeen(Device $device, string $ip): void
    {
        $device->update([
            'last_seen_at' => now(),
            'ip_address'   => $ip,
        ]);
    }

    public function paginate(int $perPage = 20, array $filters = []): LengthAwarePaginator
    {
        $query = Device::with('user')
            ->latest('last_seen_at');

        if (!empty($filters['search'])) {
            $search = $filters['search'];
            $query->where(function ($q) use ($search) {
                $q->where('device_token', 'ilike', "%{$search}%")
                  ->orWhere('app_version', 'ilike', "%{$search}%")
                  ->orWhere('ip_address', 'ilike', "%{$search}%");
            });
        }

        if (isset($filters['is_blocked'])) {
            $query->where('is_blocked', (bool) $filters['is_blocked']);
        }

        if (!empty($filters['app_version'])) {
            $query->where('app_version', $filters['app_version']);
        }

        return $query->paginate($perPage);
    }
}
