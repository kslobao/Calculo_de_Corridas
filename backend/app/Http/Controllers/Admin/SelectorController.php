<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\SelectorStoreRequest;
use App\Models\Selector;
use App\Models\SelectorVersion;
use App\Repositories\Contracts\SelectorRepositoryInterface;
use App\Services\AuditService;
use App\Services\SelectorService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class SelectorController extends Controller
{
    public function __construct(
        private readonly SelectorRepositoryInterface $selectorRepository,
        private readonly SelectorService $selectorService,
        private readonly AuditService $auditService,
    ) {}

    public function index(Request $request): View
    {
        $activeVersion = $this->selectorRepository->activeVersion();
        $versions      = SelectorVersion::orderByDesc('version')->get();
        $versionId     = $request->query('version_id', $activeVersion?->id);
        $selectors     = $versionId
            ? Selector::where('version_id', $versionId)->orderBy('app_key')->orderBy('field_type')->orderByDesc('priority')->get()
            : collect();

        return view('admin.selectors.index', compact('versions', 'activeVersion', 'selectors', 'versionId'));
    }

    public function create(): View
    {
        $versions = SelectorVersion::orderByDesc('version')->get();
        return view('admin.selectors.create', compact('versions'));
    }

    public function store(SelectorStoreRequest $request): RedirectResponse
    {
        $admin    = $request->attributes->get('admin_user');
        $selector = Selector::create($request->validated());

        $this->auditService->log($admin->id, 'selector.create', Selector::class, $selector->id,
            null, $selector->toArray(), $request);

        return redirect()->route('admin.selectors.index')->with('success', 'Seletor criado.');
    }

    public function edit(Selector $selector): View
    {
        $versions = SelectorVersion::orderByDesc('version')->get();
        return view('admin.selectors.edit', compact('selector', 'versions'));
    }

    public function update(SelectorStoreRequest $request, Selector $selector): RedirectResponse
    {
        $admin = $request->attributes->get('admin_user');
        $old   = $selector->toArray();
        $selector->update($request->validated());

        $this->auditService->log($admin->id, 'selector.update', Selector::class, $selector->id,
            $old, $selector->fresh()->toArray(), $request);

        return redirect()->route('admin.selectors.index')->with('success', 'Seletor atualizado.');
    }

    public function destroy(Request $request, Selector $selector): RedirectResponse
    {
        $admin = $request->attributes->get('admin_user');
        $this->auditService->log($admin->id, 'selector.delete', Selector::class, $selector->id,
            $selector->toArray(), null, $request);
        $selector->delete();

        return redirect()->route('admin.selectors.index')->with('success', 'Seletor excluído.');
    }

    public function duplicate(Request $request, Selector $selector): RedirectResponse
    {
        $admin  = $request->attributes->get('admin_user');
        $copy   = $selector->replicate();
        $copy->save();

        $this->auditService->log($admin->id, 'selector.duplicate', Selector::class, $copy->id,
            null, $copy->toArray(), $request);

        return redirect()->route('admin.selectors.index')->with('success', 'Seletor duplicado.');
    }

    public function publish(Request $request): RedirectResponse
    {
        $request->validate(['description' => 'required|string|max:500']);
        $admin   = $request->attributes->get('admin_user');

        if (!$admin->canEdit()) {
            return back()->withErrors(['permission' => 'Sem permissão para publicar.']);
        }

        $version = $this->selectorService->publishNewVersion(
            $request->input('description'),
            $admin->id,
        );

        $this->auditService->log($admin->id, 'selector.publish', SelectorVersion::class, (string) $version->id,
            null, ['version' => $version->version], $request);

        return redirect()->route('admin.selectors.index')
            ->with('success', "Versão {$version->version} publicada com sucesso.");
    }

    public function rollback(Request $request): RedirectResponse
    {
        $admin   = $request->attributes->get('admin_user');

        if (!$admin->canDelete()) {
            return back()->withErrors(['permission' => 'Sem permissão para rollback.']);
        }

        $version = $this->selectorService->rollback();

        if (!$version) {
            return back()->withErrors(['version' => 'Nenhuma versão anterior disponível.']);
        }

        $this->auditService->log($admin->id, 'selector.rollback', SelectorVersion::class, (string) $version->id,
            null, ['version' => $version->version], $request);

        return redirect()->route('admin.selectors.index')
            ->with('success', "Rollback para versão {$version->version} realizado.");
    }
}
