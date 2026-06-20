<?php

namespace App\Services;

use App\Enums\LicensePlan;
use App\Enums\LicenseSource;
use App\Enums\SubscriptionStatus;
use App\Models\Device;
use App\Models\License;
use App\Models\Subscription;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class SubscriptionService
{
    public function __construct(
        private readonly GooglePlayService $googlePlayService,
    ) {}

    public function validateAndSync(Device $device, string $productId, string $purchaseToken): ?License
    {
        $googleData = $this->googlePlayService->getSubscription($productId, $purchaseToken);

        if (!$googleData) {
            Log::warning('SubscriptionService: Google returned null', compact('productId', 'purchaseToken'));
            return null;
        }

        return DB::transaction(function () use ($device, $productId, $purchaseToken, $googleData) {
            $isActive   = $this->googlePlayService->isSubscriptionActive($googleData);
            $expiresAt  = $this->googlePlayService->extractExpiryDate($googleData);
            $startedAt  = $this->googlePlayService->extractStartTime($googleData);
            $orderId    = $this->googlePlayService->extractOrderId($googleData);
            $status     = $isActive ? SubscriptionStatus::Active : SubscriptionStatus::Expired;

            $license = License::updateOrCreate(
                ['purchase_token' => $purchaseToken],
                [
                    'device_id'      => $device->id,
                    'user_id'        => $device->user_id,
                    'plan'           => $isActive ? LicensePlan::Pro->value : LicensePlan::Free->value,
                    'source'         => LicenseSource::Google->value,
                    'is_active'      => $isActive,
                    'expires_at'     => $expiresAt,
                    'product_id'     => $productId,
                    'purchase_token' => $purchaseToken,
                ]
            );

            Subscription::updateOrCreate(
                ['purchase_token' => $purchaseToken],
                [
                    'license_id'          => $license->id,
                    'product_id'          => $productId,
                    'google_order_id'     => $orderId,
                    'status'              => $status->value,
                    'started_at'          => $startedAt,
                    'expires_at'          => $expiresAt,
                    'last_validated_at'   => now(),
                    'raw_google_response' => $googleData,
                ]
            );

            return $isActive ? $license : null;
        });
    }

    public function handleRtdnUpdate(
        string $purchaseToken,
        string $productId,
        SubscriptionStatus $newStatus,
        array $rawGoogleData,
    ): void {
        DB::transaction(function () use ($purchaseToken, $productId, $newStatus, $rawGoogleData) {
            $sub = Subscription::where('purchase_token', $purchaseToken)->first();

            if (!$sub) {
                $license = License::create([
                    'plan'           => LicensePlan::Pro->value,
                    'source'         => LicenseSource::Google->value,
                    'is_active'      => $newStatus->isActive(),
                    'purchase_token' => $purchaseToken,
                    'product_id'     => $productId,
                    'expires_at'     => $this->googlePlayService->extractExpiryDate($rawGoogleData),
                ]);

                $sub = Subscription::create([
                    'license_id'          => $license->id,
                    'product_id'          => $productId,
                    'purchase_token'      => $purchaseToken,
                    'status'              => $newStatus->value,
                    'started_at'          => $this->googlePlayService->extractStartTime($rawGoogleData),
                    'expires_at'          => $this->googlePlayService->extractExpiryDate($rawGoogleData),
                    'last_validated_at'   => now(),
                    'raw_google_response' => $rawGoogleData,
                ]);
            } else {
                $expiresAt = $this->googlePlayService->extractExpiryDate($rawGoogleData);

                $sub->update([
                    'status'              => $newStatus->value,
                    'expires_at'          => $expiresAt,
                    'cancelled_at'        => in_array($newStatus, [SubscriptionStatus::Cancelled, SubscriptionStatus::Revoked]) ? now() : null,
                    'last_validated_at'   => now(),
                    'raw_google_response' => $rawGoogleData,
                ]);

                $sub->license?->update([
                    'is_active'  => $newStatus->isActive(),
                    'plan'       => $newStatus->isActive() ? LicensePlan::Pro->value : LicensePlan::Free->value,
                    'expires_at' => $expiresAt,
                ]);
            }
        });
    }
}
