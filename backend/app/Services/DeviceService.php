<?php

namespace App\Services;

use App\Models\Device;
use App\Repositories\Contracts\DeviceRepositoryInterface;

class DeviceService
{
    public function __construct(
        private readonly DeviceRepositoryInterface $deviceRepository,
    ) {}

    public function registerOrUpdate(array $data, string $ip): Device
    {
        $device = $this->deviceRepository->createOrUpdate($data);
        $this->deviceRepository->touchSeen($device, $ip);
        return $device;
    }

    public function block(Device $device, string $reason, int $adminUserId): void
    {
        $device->update([
            'is_blocked'     => true,
            'blocked_reason' => $reason,
        ]);
    }

    public function unblock(Device $device): void
    {
        $device->update([
            'is_blocked'     => false,
            'blocked_reason' => null,
        ]);
    }

    public function getPlan(Device $device): string
    {
        $license = $device->activeLicense();
        return $license?->plan->value ?? 'free';
    }
}
