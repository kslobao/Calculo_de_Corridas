@extends('admin.layouts.app')
@section('title', 'Seletores Remotos')

@section('content')
<div class="d-flex align-items-center justify-content-between mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-code-slash me-2 text-primary"></i>Seletores Remotos</h5>
    <div class="d-flex gap-2">
        <a href="{{ route('admin.selectors.create') }}" class="btn btn-primary btn-sm">
            <i class="bi bi-plus-lg me-1"></i> Novo Seletor
        </a>
        <button class="btn btn-success btn-sm" data-bs-toggle="modal" data-bs-target="#publishModal">
            <i class="bi bi-cloud-upload me-1"></i> Publicar Versão
        </button>
        <form method="POST" action="{{ route('admin.selectors.rollback') }}">
            @csrf
            <button class="btn btn-outline-warning btn-sm" onclick="return confirm('Confirmar rollback?')">
                <i class="bi bi-arrow-counterclockwise me-1"></i> Rollback
            </button>
        </form>
    </div>
</div>

<div class="row mb-3">
    <div class="col-md-5">
        <div class="card shadow-sm bg-primary text-white">
            <div class="card-body py-2">
                <div class="small opacity-75">Versão ativa</div>
                <div class="fs-4 fw-bold">v{{ $activeVersion?->version ?? '—' }}</div>
                <div class="small opacity-75">{{ $activeVersion?->description ?? 'Nenhuma versão publicada' }}</div>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <form method="GET" class="d-flex gap-2 align-items-center h-100">
            <select name="version_id" class="form-select form-select-sm" onchange="this.form.submit()">
                @foreach($versions as $v)
                    <option value="{{ $v->id }}" {{ $versionId == $v->id ? 'selected' : '' }}>
                        v{{ $v->version }}{{ $v->is_active ? ' ✓ ativa' : '' }}
                    </option>
                @endforeach
            </select>
        </form>
    </div>
</div>

<div class="card shadow-sm">
    <div class="card-body p-0">
        <table class="table table-hover mb-0" id="selectors-table">
            <thead class="table-light">
                <tr><th>App</th><th>Campo</th><th>Tipo</th><th>Padrão</th><th>Prioridade</th><th>Ativa</th><th>Ações</th></tr>
            </thead>
            <tbody>
            @forelse($selectors as $sel)
                <tr>
                    <td><span class="badge" style="background:{{ $sel->app_key->badgeColor() }}; color:{{ in_array($sel->app_key->value,['99','indrive']) ? '#000' : '#fff' }}">{{ $sel->app_key->value }}</span></td>
                    <td class="small">{{ $sel->field_type->label() }}</td>
                    <td><span class="badge bg-light text-dark border">{{ $sel->selector_type->value }}</span></td>
                    <td><code class="small text-break" style="max-width:250px;display:inline-block">{{ Str::limit($sel->pattern_value, 60) }}</code></td>
                    <td><span class="badge bg-secondary">{{ $sel->priority }}</span></td>
                    <td>{{ $sel->is_active ? '✅' : '❌' }}</td>
                    <td class="d-flex gap-1">
                        <a href="{{ route('admin.selectors.edit', $sel) }}" class="btn btn-xs btn-outline-primary btn-sm py-0 px-2"><i class="bi bi-pencil"></i></a>
                        <form method="POST" action="{{ route('admin.selectors.duplicate', $sel) }}" class="d-inline">
                            @csrf
                            <button class="btn btn-xs btn-outline-secondary btn-sm py-0 px-2" title="Duplicar"><i class="bi bi-copy"></i></button>
                        </form>
                        <form method="POST" action="{{ route('admin.selectors.destroy', $sel) }}" class="d-inline" onsubmit="return confirm('Excluir?')">
                            @csrf @method('DELETE')
                            <button class="btn btn-xs btn-outline-danger btn-sm py-0 px-2"><i class="bi bi-trash"></i></button>
                        </form>
                    </td>
                </tr>
            @empty
                <tr><td colspan="7" class="text-center text-muted py-4">Nenhum seletor encontrado para esta versão.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
</div>

<!-- Publish Modal -->
<div class="modal fade" id="publishModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-cloud-upload me-2"></i>Publicar Nova Versão</h5>
                <button class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form method="POST" action="{{ route('admin.selectors.publish') }}">
                @csrf
                <div class="modal-body">
                    <p class="text-muted">Todos os seletores ativos da versão atual serão copiados para uma nova versão. Os dispositivos receberão os novos seletores automaticamente.</p>
                    <label class="form-label fw-semibold">Descrição da versão *</label>
                    <textarea name="description" class="form-control" rows="2" required placeholder="Ex: Atualização para Uber 4.x - novo ViewId do valor"></textarea>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-success"><i class="bi bi-cloud-upload me-1"></i> Publicar</button>
                    <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection

@push('scripts')
@if($selectors->count() > 0)
<script>$('#selectors-table').DataTable({ pageLength: 25, language: { url: '//cdn.datatables.net/plug-ins/1.13.8/i18n/pt-BR.json' } });</script>
@endif
@endpush
