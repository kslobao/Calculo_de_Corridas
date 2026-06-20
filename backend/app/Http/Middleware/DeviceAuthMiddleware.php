<?php

namespace App\Http\Middleware;

use App\Exceptions\DeviceBlockedException;
use App\Exceptions\InvalidDeviceTokenException;
use App\Repositories\Contracts\DeviceRepositoryInterface;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class DeviceAuthMiddleware
{
    public function __construct(
        private readonly DeviceRepositoryInterface $deviceRepository,
    ) {}

    public function handle(Request $request, Closure $next): Response
    {
        $authHeader = $request->header('Authorization', '');

        if (!str_starts_with($authHeader, 'Bearer ')) {
            throw new InvalidDeviceTokenException();
        }

        $token  = substr($authHeader, 7);
        $device = $this->deviceRepository->findByToken($token);

        if (!$device) {
            throw new InvalidDeviceTokenException();
        }

        if ($device->is_blocked) {
            throw new DeviceBlockedException($device->blocked_reason ?? 'Device is blocked.');
        }

        $this->deviceRepository->touchSeen($device, $request->ip());

        $request->attributes->set('device', $device);

        return $next($request);
    }
}
