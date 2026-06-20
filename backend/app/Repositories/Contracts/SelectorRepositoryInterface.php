<?php

namespace App\Repositories\Contracts;

use App\Models\SelectorVersion;

interface SelectorRepositoryInterface
{
    public function activeVersion(): ?SelectorVersion;

    public function versionByNumber(int $version): ?SelectorVersion;

    public function publishNewVersion(string $description, int $adminUserId): SelectorVersion;

    public function rollbackToPrevious(): ?SelectorVersion;
}
