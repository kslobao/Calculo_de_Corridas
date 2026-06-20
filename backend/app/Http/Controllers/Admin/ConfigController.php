<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\ConfigUpdateRequest;
use App\Models\AppConfig;
use App\Services\AuditService;
use App\Services\ConfigService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class ConfigController extends Controller
{
    public function __construct(
        private readonly ConfigService $configService,
        private readonly AuditService $auditService,
    ) {}

    public function index(): View
    {
        $configs = $this->configService->getAll();
        return view('admin.config.index', compact('configs'));
    }

    public function update(ConfigUpdateRequest $request, string $key): RedirectResponse
    {
        $admin  = $request->attributes->get('admin_user');
        $old    = AppConfig::where('config_key', $key)->first();
        $oldVal = $old?->config_value;

        $this->configService->update($key, $request->input('config_value'), $admin->id);

        $this->auditService->log($admin->id, 'config.update', AppConfig::class, $key,
            ['value' => $oldVal], ['value' => $request->input('config_value')], $request);

        return back()->with('success', "Configuração '{$key}' atualizada.");
    }
}
