<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\Api\SubscriptionValidateRequest;
use App\Models\Device;
use App\Services\SubscriptionService;
use Illuminate\Http\JsonResponse;

class SubscriptionController extends Controller
{
    public function __construct(private readonly SubscriptionService $subscriptionService) {}

    public function validate(SubscriptionValidateRequest $request): JsonResponse
    {
        /** @var Device $device */
        $device  = $request->attributes->get('device');
        $license = $this->subscriptionService->validateAndSync(
            $device,
            $request->input('product_id'),
            $request->input('purchase_token'),
        );

        if (!$license) {
            return response()->json([
                'valid'   => false,
                'reason'  => 'subscription_invalid_or_expired',
            ], 422);
        }

        return response()->json([
            'valid'      => true,
            'plan'       => $license->plan->value,
            'source'     => $license->source->value,
            'expires_at' => $license->expires_at?->toIso8601String(),
            'features'   => $license->plan->features(),
        ]);
    }
}
