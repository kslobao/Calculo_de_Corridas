<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\Device;
use App\Models\License;
use App\Models\ParserReport;
use App\Models\Subscription;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class DashboardController extends Controller
{
    public function index(): View
    {
        return view('admin.dashboard.index');
    }

    public function stats(): JsonResponse
    {
        $totalUsers       = User::count();
        $totalDevices     = Device::count();
        $proLicenses      = License::where('plan', 'pro')->where('is_active', true)->whereNull('deleted_at')->count();
        $activeSubsCount  = Subscription::whereIn('status', ['active', 'grace_period'])->count();

        $devicesLast30 = Device::selectRaw("DATE(last_seen_at) as date, COUNT(*) as count")
            ->where('last_seen_at', '>=', now()->subDays(30))
            ->groupBy('date')
            ->orderBy('date')
            ->get()
            ->pluck('count', 'date');

        $failuresByApp = ParserReport::selectRaw("app_key, COUNT(*) as count")
            ->where('success', false)
            ->where('created_at', '>=', now()->subDays(7))
            ->groupBy('app_key')
            ->get()
            ->pluck('count', 'app_key');

        $latestDevices = Device::with('user')
            ->latest('last_seen_at')
            ->limit(10)
            ->get();

        return response()->json([
            'totals' => [
                'users'             => $totalUsers,
                'devices'           => $totalDevices,
                'pro_licenses'      => $proLicenses,
                'active_subscriptions' => $activeSubsCount,
            ],
            'devices_last_30_days'  => $devicesLast30,
            'failures_by_app'       => $failuresByApp,
            'latest_devices'        => $latestDevices->map(fn($d) => [
                'id'           => $d->id,
                'app_version'  => $d->app_version,
                'ip_address'   => $d->ip_address,
                'last_seen_at' => $d->last_seen_at?->diffForHumans(),
                'is_blocked'   => $d->is_blocked,
            ]),
        ]);
    }
}
