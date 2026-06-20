<?php

namespace App\Jobs;

use App\Models\License;
use App\Services\SubscriptionService;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;

class ValidateGoogleSubscription implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public int $tries   = 2;
    public int $backoff = 60;

    public function __construct(private readonly string $licenseId)
    {
        $this->onQueue('default');
    }

    public function handle(SubscriptionService $subscriptionService): void
    {
        $license = License::find($this->licenseId);

        if (!$license || !$license->purchase_token || !$license->product_id) {
            Log::warning('ValidateGoogleSubscription: license not found or missing token', [
                'license_id' => $this->licenseId,
            ]);
            return;
        }

        $device = $license->device;

        if (!$device) {
            Log::warning('ValidateGoogleSubscription: no device for license', [
                'license_id' => $this->licenseId,
            ]);
            return;
        }

        $subscriptionService->validateAndSync(
            $device,
            $license->product_id,
            $license->purchase_token,
        );
    }
}
