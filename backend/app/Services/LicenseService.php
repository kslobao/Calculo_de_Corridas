<?php

namespace App\Services;

use App\Enums\LicensePlan;
use App\Enums\LicenseSource;
use App\Models\Device;
use App\Models\License;
use App\Repositories\Contracts\LicenseRepositoryInterface;

class LicenseService
{
    public function __construct(
        private readonly LicenseRepositoryInterface $licenseRepository,
        private readonly SubscriptionService $subscriptionService,
    ) {}

    public function check(Device $device, ?string $purchaseToken): array
    {
        // Prioridade 1: licença manual ativa (gift, partner, beta, admin)
        $manualLicense = $this->licenseRepository->findManualForDevice($device);
        if ($manualLicense) {
            return $this->buildResponse($manualLicense);
        }

        // Prioridade 2: validar/sync Google Play se token fornecido
        if ($purchaseToken) {
            $productId = $this->resolveProductId($purchaseToken, $device);
            if ($productId) {
                $googleLicense = $this->subscriptionService->validateAndSync($device, $productId, $purchaseToken);
                if ($googleLicense) {
                    return $this->buildResponse($googleLicense);
                }
            }
        }

        // Prioridade 3: licença Google já existente e válida
        $googleLicense = $this->licenseRepository->findGoogleForDevice($device);
        if ($googleLicense) {
            return $this->buildResponse($googleLicense);
        }

        // Default: FREE
        return $this->buildFreeResponse();
    }

    public function createManual(array $data): License
    {
        return $this->licenseRepository->create(array_merge($data, [
            'plan'      => LicensePlan::Pro->value,
            'is_active' => true,
        ]));
    }

    public function update(License $license, array $data): License
    {
        return $this->licenseRepository->update($license, $data);
    }

    public function block(License $license, string $reason): void
    {
        $this->licenseRepository->update($license, [
            'is_active' => false,
            'reason'    => $reason,
        ]);
    }

    public function unblock(License $license): void
    {
        $this->licenseRepository->update($license, ['is_active' => true]);
    }

    private function resolveProductId(string $purchaseToken, Device $device): ?string
    {
        $existing = License::where('purchase_token', $purchaseToken)->first();
        if ($existing?->product_id) {
            return $existing->product_id;
        }
        return config('services.google_play.default_product_id', 'pro_monthly');
    }

    private function buildResponse(License $license): array
    {
        return [
            'active'     => $license->isPro(),
            'plan'       => $license->plan->value,
            'source'     => $license->source->value,
            'expires_at' => $license->expires_at?->toIso8601String(),
            'reason'     => $license->reason,
            'message'    => null,
            'features'   => $license->plan->features(),
        ];
    }

    private function buildFreeResponse(): array
    {
        return [
            'active'     => true,
            'plan'       => LicensePlan::Free->value,
            'source'     => LicenseSource::Free->value,
            'expires_at' => null,
            'reason'     => null,
            'message'    => null,
            'features'   => LicensePlan::Free->features(),
        ];
    }
}
