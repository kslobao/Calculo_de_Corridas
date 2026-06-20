<?php

namespace App\Jobs;

use App\Enums\LicensePlan;
use App\Enums\SubscriptionStatus;
use App\Models\License;
use App\Models\Subscription;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class SyncExpiredSubscriptionsJob implements ShouldQueue
{
    use Dispatchable, InteractsWithQueue, Queueable, SerializesModels;

    public function handle(): void
    {
        $count = 0;

        Subscription::where('status', SubscriptionStatus::Active->value)
            ->where('expires_at', '<', now())
            ->with('license')
            ->chunkById(100, function ($subscriptions) use (&$count) {
                foreach ($subscriptions as $sub) {
                    DB::transaction(function () use ($sub, &$count) {
                        $sub->update(['status' => SubscriptionStatus::Expired->value]);

                        if ($sub->license && $sub->license->source->value === 'google') {
                            $sub->license->update([
                                'is_active' => false,
                                'plan'      => LicensePlan::Free->value,
                            ]);
                        }

                        $count++;
                    });
                }
            });

        Log::info("SyncExpiredSubscriptionsJob: marked {$count} subscriptions as expired.");
    }
}
