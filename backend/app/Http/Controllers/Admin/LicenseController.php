<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\LicenseStoreRequest;
use App\Http\Requests\Admin\LicenseUpdateRequest;
use App\Models\Device;
use App\Models\License;
use App\Repositories\Contracts\LicenseRepositoryInterface;
use App\Services\AuditService;
use App\Services\LicenseService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class LicenseController extends Controller
{
    public function __construct(
        private readonly LicenseRepositoryInterface $licenseRepository,
        private readonly LicenseService $licenseService,
        private readonly AuditService $auditService,
    ) {}

    public function index(Request $request): View
    {
        $licenses = $this->licenseRepository->paginate(20, $request->only(['plan', 'source', 'is_active', 'search']));
        return view('admin.licenses.index', compact('licenses'));
    }

    public function create(): View
    {
        return view('admin.licenses.create');
    }

    public function store(LicenseStoreRequest $request): RedirectResponse
    {
        $admin   = $request->attributes->get('admin_user');
        $data    = array_merge($request->validated(), ['created_by' => $admin->id]);
        $license = $this->licenseService->createManual($data);

        $this->auditService->log($admin->id, 'license.create', License::class, $license->id,
            null, $license->toArray(), $request);

        return redirect()->route('admin.licenses.show', $license)->with('success', 'Licença criada.');
    }

    public function show(License $license): View
    {
        $license->load(['device', 'user', 'subscriptions', 'createdByAdmin']);
        return view('admin.licenses.show', compact('license'));
    }

    public function edit(License $license): View
    {
        return view('admin.licenses.edit', compact('license'));
    }

    public function update(LicenseUpdateRequest $request, License $license): RedirectResponse
    {
        $admin  = $request->attributes->get('admin_user');
        $old    = $license->toArray();
        $this->licenseService->update($license, $request->validated());

        $this->auditService->log($admin->id, 'license.update', License::class, $license->id,
            $old, $license->fresh()->toArray(), $request);

        return back()->with('success', 'Licença atualizada.');
    }

    public function block(Request $request, License $license): RedirectResponse
    {
        $request->validate(['reason' => 'required|string|max:500']);
        $admin = $request->attributes->get('admin_user');
        $this->licenseService->block($license, $request->input('reason'));
        $this->auditService->log($admin->id, 'license.block', License::class, $license->id,
            ['is_active' => true], ['is_active' => false], $request);

        return back()->with('success', 'Licença bloqueada.');
    }

    public function unblock(Request $request, License $license): RedirectResponse
    {
        $admin = $request->attributes->get('admin_user');
        $this->licenseService->unblock($license);
        $this->auditService->log($admin->id, 'license.unblock', License::class, $license->id,
            ['is_active' => false], ['is_active' => true], $request);

        return back()->with('success', 'Licença reativada.');
    }
}
