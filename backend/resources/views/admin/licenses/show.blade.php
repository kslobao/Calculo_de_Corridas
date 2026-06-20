@extends('admin.layouts.app')
@section('title', 'Licença')

@section('content')
<div class="row g-3">
    <div class="col-lg-5">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3 d-flex justify-content-between align-items-center">
                <span><i class="bi bi-key me-2 text-primary"></i>Licença</span>
                <div class="d-flex gap-1">
                    @if($license->is_active && !$license->trashed())
                        <button class="btn btn-sm btn-outline-danger" data-bs-toggle="modal" data-bs-target="#blockModal">
                            <i class="bi bi-lock"></i> Bloquear
                        </button>
                    @else
                        <form method="POST" action="{{ route('admin.licenses.unblock', $license) }}">
                            @csrf
                            <button class="btn btn-sm btn-outline-success"><i class="bi bi-unlock"></i> Desbloquear</button>
                        </form>
                    @endif
                </div>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <tr><th class="text-muted fw-normal w-40">ID</th><td><code class="small">{{ $license->id }}</code></td></tr>
                    <tr><th class="text-muted fw-normal">Plano</th><td>
                        @if($license->plan->value === 'pro')
                            <span class="badge bg-warning text-dark"><i class="bi bi-star-fill me-1"></i>PRO</span>
                        @else
                            <span class="badge bg-secondary">Free</span>
                        @endif
                    </td></tr>
                    <tr><th class="text-muted fw-normal">Origem</th><td><span class="badge badge-source-{{ $license->source->value }}">{{ $license->source->label() }}</span></td></tr>
                    <tr><th class="text-muted fw-normal">Ativa</th><td>{{ $license->is_active ? '✅ Sim' : '❌ Não' }}</td></tr>
                    <tr><th class="text-muted fw-normal">Expira</th><td>{{ $license->expires_at ? $license->expires_at->format('d/m/Y H:i') : 'Nunca (vitalícia)' }}</td></tr>
                    <tr><th class="text-muted fw-normal">Motivo</th><td>{{ $license->reason ?? '—' }}</td></tr>
                    <tr><th class="text-muted fw-normal">Criada em</th><td>{{ $license->created_at->format('d/m/Y H:i') }}</td></tr>
                    @if($license->createdByAdmin)
                    <tr><th class="text-muted fw-normal">Criada por</th><td>{{ $license->createdByAdmin->name }}</td></tr>
                    @endif
                </table>
            </div>
        </div>

        @if($license->device)
        <div class="card shadow-sm mt-3">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-phone me-1 text-info"></i> Dispositivo vinculado
            </div>
            <div class="card-body small text-muted">
                <div>ID: <code>{{ $license->device->id }}</code></div>
                <div>Token: <code>{{ substr($license->device->device_token, 0, 16) }}…</code></div>
                <div>Versão: {{ $license->device->app_version ?? '—' }}</div>
                <div>Bloqueado: {{ $license->device->is_blocked ? '❌ Sim' : '✅ Não' }}</div>
            </div>
        </div>
        @endif
    </div>

    <div class="col-lg-7">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-credit-card me-1 text-success"></i> Assinaturas Google Play
            </div>
            <div class="card-body p-0">
                <table class="table table-sm mb-0">
                    <thead class="table-light">
                        <tr><th>Produto</th><th>Status</th><th>Expira</th><th>Validado</th></tr>
                    </thead>
                    <tbody>
                    @forelse($license->subscriptions as $sub)
                        <tr>
                            <td class="small">{{ $sub->product_id }}</td>
                            <td><span class="badge bg-{{ $sub->status->badgeClass() }}">{{ $sub->status->label() }}</span></td>
                            <td class="small">{{ $sub->expires_at?->format('d/m/Y') ?? '—' }}</td>
                            <td class="small text-muted">{{ $sub->last_validated_at?->diffForHumans() ?? '—' }}</td>
                        </tr>
                    @empty
                        <tr><td colspan="4" class="text-center text-muted py-3">Nenhuma assinatura vinculada.</td></tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- Block Modal -->
<div class="modal fade" id="blockModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Bloquear Licença</h5>
                <button class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form method="POST" action="{{ route('admin.licenses.block', $license) }}">
                @csrf
                <div class="modal-body">
                    <label class="form-label">Motivo do bloqueio *</label>
                    <textarea name="reason" class="form-control" rows="3" required placeholder="Descreva o motivo…"></textarea>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-danger">Bloquear</button>
                    <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection
