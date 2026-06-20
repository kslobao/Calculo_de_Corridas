<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\Api\LicenseCheckRequest;
use App\Models\Device;
use App\Services\LicenseService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class LicenseController extends Controller
{
    public function __construct(private readonly LicenseService $licenseService) {}

    public function check(LicenseCheckRequest $request): JsonResponse
    {
        /** @var Device $device */
        $device = $request->attributes->get('device');

        $result = $this->licenseService->check(
            $device,
            $request->input('purchase_token'),
        );

        return response()->json($result);
    }
}
