@extends('admin.layouts.app')
@section('title', 'Usuário')

@section('content')
<div class="d-flex align-items-center justify-content-between mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-person me-2 text-primary"></i>Usuário</h5>
    <a href="{{ route('admin.users.index') }}" class="btn btn-outline-secondary btn-sm">
        <i class="bi bi-arrow-left me-1"></i> Voltar
    </a>
</div>

<div class="row g-3">
    <div class="col-lg-4">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-person-circle me-2 text-primary"></i>Perfil
            </div>
            <div class="card-body">
                <table class="table table-sm table-borderless mb-0">
                    <tr><th class="text-muted small w-35">ID</th>
                        <td><code class="small">{{ substr($user->id, 0, 8) }}…</code></td></tr>
                    <tr><th class="text-muted small">Email</th>
                        <td class="small">{{ $user->email ?? '—' }}</td></tr>
                    <tr><th class="text-muted small">Google ID</th>
                        <td><code class="small">{{ $user->google_id ?? '—' }}</code></td></tr>
                    <tr><th class="text-muted small">Criado</th>
                        <td class="small">{{ $user->created_at->format('d/m/Y H:i') }}</td></tr>
                </table>
            </div>
        </div>

        <div class="card shadow-sm mt-3">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-shield-check me-2 text-success"></i>Licença
            </div>
            <div class="card-body">
                @php $license = $user->activeLicense(); @endphp
                @if($license)
                    <table class="table table-sm table-borderless mb-0">
                        <tr><th class="text-muted small">Plano</th>
                            <td><span class="badge {{ $license->plan->value === 'pro' ? 'bg-warning text-dark' : 'bg-secondary' }}">{{ strtoupper($license->plan->value) }}</span></td></tr>
                        <tr><th class="text-muted small">Origem</th>
                            <td><span class="badge bg-info text-dark">{{ $license->source->value }}</span></td></tr>
                        <tr><th class="text-muted small">Expira</th>
                            <td class="small">{{ $license->expires_at ? $license->expires_at->format('d/m/Y') : 'Nunca' }}</td></tr>
                    </table>
                @else
                    <p class="text-muted mb-0 small">Sem licença ativa.</p>
                @endif
            </div>
        </div>
    </div>

    <div class="col-lg-8">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-phone me-2 text-info"></i>Dispositivos ({{ $user->devices->count() }})
            </div>
            <div class="card-body p-0">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>Token (início)</th><th>Versão</th><th>IP</th><th>Última vez</th><th>Status</th></tr></thead>
                    <tbody>
                    @forelse($user->devices as $device)
                        <tr class="{{ $device->is_blocked ? 'table-danger' : '' }}">
                            <td><code class="small">{{ substr($device->device_token, 0, 12) }}…</code></td>
                            <td><span class="badge bg-secondary small">{{ $device->app_version ?? '?' }}</span></td>
                            <td class="small text-muted">{{ $device->ip_address ?? '—' }}</td>
                            <td class="small">{{ $device->last_seen_at?->diffForHumans() }}</td>
                            <td>{{ $device->is_blocked ? '🔴' : '🟢' }}</td>
                        </tr>
                    @empty
                        <tr><td colspan="5" class="text-center text-muted py-3">Nenhum dispositivo.</td></tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card shadow-sm mt-3">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-credit-card me-2 text-warning"></i>Assinaturas Google Play
            </div>
            <div class="card-body p-0">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>Produto</th><th>Status</th><th>Expira</th><th>Atualizado</th></tr></thead>
                    <tbody>
                    @forelse($user->subscriptions as $sub)
                        <tr>
                            <td class="small"><code>{{ $sub->product_id }}</code></td>
                            <td>
                                @php $isActive = $sub->status->isActive(); @endphp
                                <span class="badge {{ $isActive ? 'bg-success' : 'bg-secondary' }}">{{ $sub->status->value }}</span>
                            </td>
                            <td class="small">{{ $sub->expires_at?->format('d/m/Y') ?? '—' }}</td>
                            <td class="small text-muted">{{ $sub->updated_at->diffForHumans() }}</td>
                        </tr>
                    @empty
                        <tr><td colspan="4" class="text-center text-muted py-3">Nenhuma assinatura.</td></tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
@endsection
