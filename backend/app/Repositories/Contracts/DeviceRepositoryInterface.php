<?php

namespace App\Repositories\Contracts;

use App\Models\Device;

interface DeviceRepositoryInterface
{
    public function findByToken(string $token): ?Device;

    public function createOrUpdate(array $data): Device;

    public function touchSeen(Device $device, string $ip): void;

    public function paginate(int $perPage = 20, array $filters = []): mixed;
}
