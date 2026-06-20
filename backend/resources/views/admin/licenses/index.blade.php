@extends('admin.layouts.app')
@section('title', 'Licenças')

@section('content')
<div class="d-flex align-items-center justify-content-between mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-key me-2 text-primary"></i>Licenças</h5>
    <a href="{{ route('admin.licenses.create') }}" class="btn btn-primary btn-sm">
        <i class="bi bi-plus-lg me-1"></i> Nova Licença Manual
    </a>
</div>

<div class="card shadow-sm mb-3">
    <div class="card-body">
        <form method="GET" class="row g-2">
            <div class="col-md-3">
                <select name="plan" class="form-select form-select-sm">
                    <option value="">Todos os planos</option>
                    <option value="free" {{ request('plan') === 'free' ? 'selected' : '' }}>Free</option>
                    <option value="pro"  {{ request('plan') === 'pro'  ? 'selected' : '' }}>PRO</option>
                </select>
            </div>
            <div class="col-md-3">
                <select name="source" class="form-select form-select-sm">
                    <option value="">Todas as origens</option>
                    @foreach(['free','google','gift','partner','beta','admin'] as $s)
                        <option value="{{ $s }}" {{ request('source') === $s ? 'selected' : '' }}>{{ ucfirst($s) }}</option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-3">
                <select name="is_active" class="form-select form-select-sm">
                    <option value="">Todos os status</option>
                    <option value="1" {{ request('is_active') === '1' ? 'selected' : '' }}>Ativas</option>
                    <option value="0" {{ request('is_active') === '0' ? 'selected' : '' }}>Inativas</option>
                </select>
            </div>
            <div class="col-md-2">
                <button class="btn btn-sm btn-outline-primary w-100">Filtrar</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow-sm">
    <div class="card-body p-0">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>ID</th><th>Plano</th><th>Origem</th><th>Device / User</th>
                    <th>Expira em</th><th>Ativa</th><th>Ações</th>
                </tr>
            </thead>
            <tbody>
            @forelse($licenses as $license)
                <tr>
                    <td><code class="small">{{ substr($license->id, 0, 8) }}…</code></td>
                    <td>
                        @if($license->plan->value === 'pro')
                            <span class="badge bg-warning text-dark"><i class="bi bi-star-fill me-1"></i>PRO</span>
                        @else
                            <span class="badge bg-secondary">Free</span>
                        @endif
                    </td>
                    <td>
                        <span class="badge badge-source-{{ $license->source->value }}">
                            {{ $license->source->label() }}
                        </span>
                    </td>
                    <td class="small text-muted">
                        {{ $license->device_id ? substr($license->device_id, 0, 8).'…' : '—' }}
                    </td>
                    <td class="small">
                        {{ $license->expires_at ? $license->expires_at->format('d/m/Y') : '<span class="text-success">Vitalícia</span>' }}
                    </td>
                    <td>
                        @if($license->is_active && !$license->trashed())
                            <span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Sim</span>
                        @else
                            <span class="badge bg-danger"><i class="bi bi-x-circle me-1"></i>Não</span>
                        @endif
                    </td>
                    <td>
                        <a href="{{ route('admin.licenses.show', $license) }}" class="btn btn-xs btn-outline-primary btn-sm py-0 px-2">
                            <i class="bi bi-eye"></i>
                        </a>
                    </td>
                </tr>
            @empty
                <tr><td colspan="7" class="text-center text-muted py-4">Nenhuma licença encontrada.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
    <div class="card-footer bg-white">
        {{ $licenses->withQueryString()->links() }}
    </div>
</div>
@endsection
