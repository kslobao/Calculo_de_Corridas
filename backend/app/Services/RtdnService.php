<?php

namespace App\Services;

use App\Enums\RtdnNotificationType;
use App\Enums\SubscriptionStatus;
use App\Jobs\ProcessRtdnNotification;
use Illuminate\Support\Facades\Log;

class RtdnService
{
    public function handle(array $pubSubPayload): bool
    {
        try {
            $messageData = $pubSubPayload['message']['data'] ?? null;

            if (!$messageData) {
                Log::warning('RtdnService: missing message.data in payload');
                return false;
            }

            $decoded      = base64_decode($messageData);
            $notification = json_decode($decoded, true);

            if (!$notification) {
                Log::warning('RtdnService: failed to decode notification JSON');
                return false;
            }

            $subscriptionNotification = $notification['subscriptionNotification'] ?? null;

            if (!$subscriptionNotification) {
                Log::info('RtdnService: notification without subscriptionNotification (test notification?)');
                return true;
            }

            $notificationType = (int) ($subscriptionNotification['notificationType'] ?? 0);
            $purchaseToken    = $subscriptionNotification['purchaseToken'] ?? null;
            $subscriptionId   = $subscriptionNotification['subscriptionId'] ?? null;

            if (!$purchaseToken || !$subscriptionId) {
                Log::warning('RtdnService: missing purchaseToken or subscriptionId');
                return false;
            }

            $type = RtdnNotificationType::tryFrom($notificationType);

            if (!$type) {
                Log::info('RtdnService: unknown notificationType', ['type' => $notificationType]);
                return true;
            }

            ProcessRtdnNotification::dispatch($type, $purchaseToken, $subscriptionId);

            return true;
        } catch (\Throwable $e) {
            Log::error('RtdnService: exception', ['error' => $e->getMessage()]);
            return false;
        }
    }
}
