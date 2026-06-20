<?php

namespace App\Jobs;

use App\Enums\RtdnNotificationType;
use App\Services\GooglePlayService;
use App\Services\SubscriptionService;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;

class ProcessRtdnNotification implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public int $tries   = 3;
    public int $backoff = 30;

    public function __construct(
        private readonly RtdnNotificationType $notificationType,
        private readonly string $purchaseToken,
        private readonly string $subscriptionId,
    ) {
        $this->onQueue('rtdn');
    }

    public function handle(
        GooglePlayService $googlePlayService,
        SubscriptionService $subscriptionService,
    ): void {
        Log::info('ProcessRtdnNotification', [
            'type'           => $this->notificationType->name,
            'subscription_id'=> $this->subscriptionId,
        ]);

        $googleData = $googlePlayService->getSubscription($this->subscriptionId, $this->purchaseToken);

        if (!$googleData) {
            Log::warning('ProcessRtdnNotification: Google returned null, using fallback status', [
                'type' => $this->notificationType->name,
            ]);
        }

        $targetStatus = $this->notificationType->targetSubscriptionStatus();

        $subscriptionService->handleRtdnUpdate(
            purchaseToken: $this->purchaseToken,
            productId:     $this->subscriptionId,
            newStatus:     $targetStatus,
            rawGoogleData: $googleData ?? [],
        );
    }

    public function failed(\Throwable $exception): void
    {
        Log::error('ProcessRtdnNotification: job failed', [
            'type'  => $this->notificationType->name,
            'error' => $exception->getMessage(),
        ]);
    }
}
