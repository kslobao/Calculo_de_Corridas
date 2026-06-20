<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\Device;
use App\Repositories\Contracts\DeviceRepositoryInterface;
use App\Services\AuditService;
use App\Services\DeviceService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class DeviceController extends Controller
{
    public function __construct(
        private readonly DeviceRepositoryInterface $deviceRepository,
        private readonly DeviceService $deviceService,
        private readonly AuditService $auditService,
    ) {}

    public function index(Request $request): View
    {
        $devices = $this->deviceRepository->paginate(20, $request->only(['search', 'is_blocked', 'app_version']));
        return view('admin.devices.index', compact('devices'));
    }

    public function show(Device $device): View
    {
        $device->load(['user', 'licenses', 'parserReports' => fn($q) => $q->latest()->limit(20)]);
        return view('admin.devices.show', compact('device'));
    }

    public function block(Request $request, Device $device): RedirectResponse
    {
        $request->validate(['reason' => 'required|string|max:500']);
        $admin = $request->attributes->get('admin_user');

        $this->deviceService->block($device, $request->input('reason'), $admin->id);
        $this->auditService->log($admin->id, 'device.block', Device::class, $device->id,
            ['is_blocked' => false], ['is_blocked' => true, 'reason' => $request->input('reason')], $request);

        return back()->with('success', 'Dispositivo bloqueado.');
    }

    public function unblock(Request $request, Device $device): RedirectResponse
    {
        $admin = $request->attributes->get('admin_user');
        $this->deviceService->unblock($device);
        $this->auditService->log($admin->id, 'device.unblock', Device::class, $device->id,
            ['is_blocked' => true], ['is_blocked' => false], $request);

        return back()->with('success', 'Dispositivo desbloqueado.');
    }
}
