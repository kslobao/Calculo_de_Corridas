<?php

namespace App\Http\Middleware;

use App\Exceptions\MaintenanceModeException;
use App\Services\ConfigService;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class MaintenanceModeMiddleware
{
    public function __construct(private readonly ConfigService $configService) {}

    public function handle(Request $request, Closure $next): Response
    {
        if ($this->configService->isMaintenanceMode()) {
            throw new MaintenanceModeException(
                $this->configService->getMaintenanceMessage()
            );
        }

        return $next($request);
    }
}
