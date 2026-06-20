@extends('admin.layouts.app')
@section('title', 'Logs de Auditoria')

@section('content')
<div class="d-flex align-items-center mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-shield-lock me-2 text-danger"></i>Logs de Auditoria</h5>
</div>

<div class="card shadow-sm mb-3">
    <div class="card-body py-2">
        <form method="GET" class="row g-2">
            <div class="col-md-3">
                <input type="text" name="admin" class="form-control form-control-sm" placeholder="Admin (email)…" value="{{ request('admin') }}">
            </div>
            <div class="col-md-3">
                <input type="text" name="entity" class="form-control form-control-sm" placeholder="Entidade (ex: License)…" value="{{ request('entity') }}">
            </div>
            <div class="col-md-2">
                <input type="text" name="action" class="form-control form-control-sm" placeholder="Ação…" value="{{ request('action') }}">
            </div>
            <div class="col-md-2">
                <input type="date" name="date" class="form-control form-control-sm" value="{{ request('date') }}">
            </div>
            <div class="col-md-1">
                <button class="btn btn-sm btn-outline-primary w-100">Filtrar</button>
            </div>
            @if(request()->hasAny(['admin','entity','action','date']))
            <div class="col-md-1">
                <a href="{{ route('admin.logs.index') }}" class="btn btn-sm btn-outline-secondary w-100">Limpar</a>
            </div>
            @endif
        </form>
    </div>
</div>

<div class="card shadow-sm">
    <div class="card-body p-0">
        <table class="table table-hover table-sm mb-0">
            <thead class="table-light">
                <tr><th>Data</th><th>Admin</th><th>Ação</th><th>Entidade</th><th>ID</th><th>IP</th><th>Alterações</th></tr>
            </thead>
            <tbody>
            @forelse($logs as $log)
                <tr>
                    <td class="small text-muted text-nowrap">{{ $log->created_at->format('d/m/Y H:i:s') }}</td>
                    <td class="small">{{ $log->adminUser?->email ?? '—' }}</td>
                    <td>
                        @php
                            $actionClass = match(true) {
                                str_contains($log->action, 'delete') || str_contains($log->action, 'block') => 'bg-danger',
                                str_contains($log->action, 'create') || str_contains($log->action, 'publish') => 'bg-success',
                                str_contains($log->action, 'update') || str_contains($log->action, 'unblock') => 'bg-warning text-dark',
                                default => 'bg-secondary',
                            };
                        @endphp
                        <span class="badge {{ $actionClass }} small">{{ $log->action }}</span>
                    </td>
                    <td class="small">{{ $log->entity_type }}</td>
                    <td><code class="small">{{ $log->entity_id ? substr($log->entity_id, 0, 8).'…' : '—' }}</code></td>
                    <td class="small text-muted">{{ $log->ip_address ?? '—' }}</td>
                    <td>
                        @if($log->old_values || $log->new_values)
                        <button class="btn btn-xs btn-outline-secondary btn-sm py-0 px-2"
                                data-bs-toggle="collapse" data-bs-target="#diff{{ $log->id }}">
                            <i class="bi bi-code-square"></i>
                        </button>
                        <div class="collapse mt-1" id="diff{{ $log->id }}">
                            @if($log->old_values)
                                <div class="small text-danger"><strong>Antes:</strong>
                                    <pre class="mb-0" style="font-size:.7rem">{{ json_encode($log->old_values, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) }}</pre>
                                </div>
                            @endif
                            @if($log->new_values)
                                <div class="small text-success mt-1"><strong>Depois:</strong>
                                    <pre class="mb-0" style="font-size:.7rem">{{ json_encode($log->new_values, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) }}</pre>
                                </div>
                            @endif
                        </div>
                        @else
                            <span class="text-muted small">—</span>
                        @endif
                    </td>
                </tr>
            @empty
                <tr><td colspan="7" class="text-center text-muted py-4">Nenhum log encontrado.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
    <div class="card-footer bg-white">{{ $logs->withQueryString()->links() }}</div>
</div>
@endsection
