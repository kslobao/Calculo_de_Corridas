<?php

use App\Exceptions\DeviceBlockedException;
use App\Exceptions\InvalidDeviceTokenException;
use App\Exceptions\MaintenanceModeException;
use App\Http\Middleware\AdminJwtMiddleware;
use App\Http\Middleware\DeviceAuthMiddleware;
use App\Http\Middleware\MaintenanceModeMiddleware;
use App\Http\Middleware\SecurityHeadersMiddleware;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware) {
        // Traefik termina SSL e repassa HTTP interno — confiar em todos os proxies da rede overlay
        $middleware->trustProxies(at: '*');

        $middleware->append(SecurityHeadersMiddleware::class);

        $middleware->alias([
            'device.auth' => DeviceAuthMiddleware::class,
            'admin.jwt'   => AdminJwtMiddleware::class,
            'maintenance' => MaintenanceModeMiddleware::class,
        ]);

        $middleware->throttleApi('60,1');
    })
    ->withExceptions(function (Exceptions $exceptions) {
        $exceptions->render(function (NotFoundHttpException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json(['message' => 'Resource not found.'], 404);
            }
        });

        $exceptions->render(function (InvalidDeviceTokenException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json(['message' => 'Invalid or missing device token.'], 401);
            }
        });

        $exceptions->render(function (DeviceBlockedException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'message' => 'Device is blocked.',
                    'reason'  => $e->getMessage(),
                ], 403);
            }
        });

        $exceptions->render(function (MaintenanceModeException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'message'             => 'Service under maintenance.',
                    'maintenance_message' => $e->getMessage(),
                ], 503);
            }
        });

        $exceptions->render(function (ValidationException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'message' => 'Validation failed.',
                    'errors'  => $e->errors(),
                ], 422);
            }
        });

        $exceptions->render(function (\Throwable $e, Request $request) {
            if ($request->is('api/*') && app()->environment('production')) {
                return response()->json(['message' => 'Internal server error.'], 500);
            }
        });
    })->create();
