<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\Api\DeviceRegisterRequest;
use App\Services\DeviceService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class DeviceController extends Controller
{
    public function __construct(private readonly DeviceService $deviceService) {}

    public function register(DeviceRegisterRequest $request): JsonResponse
    {
        $device  = $this->deviceService->registerOrUpdate($request->validated(), $request->ip());
        $isNew   = $device->wasRecentlyCreated;
        $plan    = $this->deviceService->getPlan($device);

        return response()->json([
            'registered' => true,
            'device_id'  => $device->id,
            'plan'       => $plan,
        ], $isNew ? 201 : 200);
    }
}
