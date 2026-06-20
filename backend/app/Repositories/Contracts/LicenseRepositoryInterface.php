<?php

namespace App\Repositories\Contracts;

use App\Models\Device;
use App\Models\License;

interface LicenseRepositoryInterface
{
    public function findForDevice(Device $device): ?License;

    public function findManualForDevice(Device $device): ?License;

    public function findGoogleForDevice(Device $device): ?License;

    public function create(array $data): License;

    public function update(License $license, array $data): License;

    public function paginate(int $perPage = 20, array $filters = []): mixed;
}
