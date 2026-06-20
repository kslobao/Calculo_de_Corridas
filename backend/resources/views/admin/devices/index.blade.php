@extends('admin.layouts.app')
@section('title', 'Dispositivos')

@section('content')
<div class="d-flex align-items-center mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-phone me-2 text-info"></i>Dispositivos</h5>
</div>

<div class="card shadow-sm mb-3">
    <div class="card-body py-2">
        <form method="GET" class="row g-2">
            <div class="col-md-5">
                <input type="text" name="search" class="form-control form-control-sm" placeholder="Buscar token, IP, versão…" value="{{ request('search') }}">
            </div>
            <div class="col-md-3">
                <select name="is_blocked" class="form-select form-select-sm">
                    <option value="">Todos</option>
                    <option value="0" {{ request('is_blocked') === '0' ? 'selected' : '' }}>Não bloqueados</option>
                    <option value="1" {{ request('is_blocked') === '1' ? 'selected' : '' }}>Bloqueados</option>
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
                <tr><th>Token (início)</th><th>Versão</th><th>IP</th><th>Última vez</th><th>Bloqueado</th><th>Ações</th></tr>
            </thead>
            <tbody>
            @forelse($devices as $device)
                <tr class="{{ $device->is_blocked ? 'table-danger' : '' }}">
                    <td><code class="small">{{ substr($device->device_token, 0, 12) }}…</code></td>
                    <td><span class="badge bg-secondary">{{ $device->app_version ?? '?' }}</span></td>
                    <td class="small text-muted">{{ $device->ip_address ?? '—' }}</td>
                    <td class="small">{{ $device->last_seen_at?->diffForHumans() }}</td>
                    <td>
                        @if($device->is_blocked)
                            <span class="badge bg-danger"><i class="bi bi-lock me-1"></i>Bloqueado</span>
                        @else
                            <span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>OK</span>
                        @endif
                    </td>
                    <td class="d-flex gap-1">
                        @if(!$device->is_blocked)
                            <button class="btn btn-xs btn-outline-danger btn-sm py-0 px-2"
                                    data-bs-toggle="modal" data-bs-target="#blockModal{{ $device->id }}">
                                <i class="bi bi-lock"></i>
                            </button>
                        @else
                            <form method="POST" action="{{ route('admin.devices.unblock', $device) }}">
                                @csrf
                                <button class="btn btn-xs btn-outline-success btn-sm py-0 px-2"><i class="bi bi-unlock"></i></button>
                            </form>
                        @endif
                    </td>
                </tr>
                <!-- Block Modal -->
                <div class="modal fade" id="blockModal{{ $device->id }}" tabindex="-1">
                    <div class="modal-dialog modal-sm">
                        <div class="modal-content">
                            <div class="modal-header"><h6 class="modal-title">Bloquear dispositivo</h6><button class="btn-close" data-bs-dismiss="modal"></button></div>
                            <form method="POST" action="{{ route('admin.devices.block', $device) }}">
                                @csrf
                                <div class="modal-body">
                                    <textarea name="reason" class="form-control" rows="2" required placeholder="Motivo…"></textarea>
                                </div>
                                <div class="modal-footer"><button class="btn btn-danger btn-sm">Bloquear</button></div>
                            </form>
                        </div>
                    </div>
                </div>
            @empty
                <tr><td colspan="6" class="text-center text-muted py-4">Nenhum dispositivo encontrado.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
    <div class="card-footer bg-white">{{ $devices->withQueryString()->links() }}</div>
</div>
@endsection
