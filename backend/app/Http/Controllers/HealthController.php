<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;

class HealthController extends Controller
{
    public function index(): JsonResponse
    {
        $db    = $this->checkDatabase();
        $redis = $this->checkRedis();
        $queue = $this->checkQueue();

        $allOk  = $db && $redis && $queue;
        $status = $allOk ? 200 : 503;

        return response()->json([
            'status'    => $allOk ? 'ok' : 'degraded',
            'timestamp' => now()->toIso8601String(),
            'services'  => [
                'database' => $db    ? 'ok' : 'error',
                'redis'    => $redis ? 'ok' : 'error',
                'queue'    => $queue ? 'ok' : 'error',
            ],
        ], $status);
    }

    public function database(): JsonResponse
    {
        $ok = $this->checkDatabase();
        return response()->json(['status' => $ok ? 'ok' : 'error'], $ok ? 200 : 503);
    }

    public function redis(): JsonResponse
    {
        $ok = $this->checkRedis();
        return response()->json(['status' => $ok ? 'ok' : 'error'], $ok ? 200 : 503);
    }

    public function queue(): JsonResponse
    {
        $ok = $this->checkQueue();
        return response()->json(['status' => $ok ? 'ok' : 'error'], $ok ? 200 : 503);
    }

    private function checkDatabase(): bool
    {
        try {
            DB::select('SELECT 1');
            return true;
        } catch (\Throwable) {
            return false;
        }
    }

    private function checkRedis(): bool
    {
        try {
            Cache::store('redis')->put('health_check', 1, 5);
            return true;
        } catch (\Throwable) {
            return false;
        }
    }

    private function checkQueue(): bool
    {
        try {
            $size = \Illuminate\Support\Facades\Redis::llen('queues:default') ?? 0;
            return true;
        } catch (\Throwable) {
            return false;
        }
    }
}
