<?php

namespace App\Services;

use App\Models\SelectorVersion;
use App\Repositories\Contracts\SelectorRepositoryInterface;
use Illuminate\Support\Facades\Cache;

class SelectorService
{
    private const CACHE_KEY    = 'selectors:active_payload';
    private const CACHE_TTL    = 300;

    public function __construct(
        private readonly SelectorRepositoryInterface $selectorRepository,
    ) {}

    public function getActiveVersion(): ?SelectorVersion
    {
        return $this->selectorRepository->activeVersion();
    }

    public function getAndroidPayload(): ?array
    {
        return Cache::remember(self::CACHE_KEY, self::CACHE_TTL, function () {
            $version = $this->selectorRepository->activeVersion();
            return $version?->buildAndroidPayload();
        });
    }

    public function isVersionCurrent(int $clientVersion): bool
    {
        $active = $this->selectorRepository->activeVersion();
        return $active !== null && $active->version === $clientVersion;
    }

    public function publishNewVersion(string $description, int $adminUserId): SelectorVersion
    {
        $version = $this->selectorRepository->publishNewVersion($description, $adminUserId);
        $this->clearCache();
        return $version;
    }

    public function rollback(): ?SelectorVersion
    {
        $version = $this->selectorRepository->rollbackToPrevious();
        $this->clearCache();
        return $version;
    }

    public function clearCache(): void
    {
        Cache::forget(self::CACHE_KEY);
    }
}
