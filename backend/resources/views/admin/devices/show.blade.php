@extends('admin.layouts.app')
@section('title', 'Dispositivo')

@section('content')
<div class="d-flex align-items-center justify-content-between mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-phone me-2 text-info"></i>Dispositivo</h5>
    <a href="{{ route('admin.devices.index') }}" class="btn btn-outline-secondary btn-sm">
        <i class="bi bi-arrow-left me-1"></i> Voltar
    </a>
</div>

<div class="row g-3">
    <div class="col-lg-5">
        <div class="card shadow-sm h-100">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-info-circle me-2 text-primary"></i>Informações do Dispositivo
            </div>
            <div class="card-body">
                <table class="table table-sm table-borderless mb-0">
                    <tr><th class="text-muted small w-35">Token</th>
                        <td><code class="small text-break">{{ $device->device_token }}</code></td></tr>
                    <tr><th class="text-muted small">Versão App</th>
                        <td><span class="badge bg-secondary">{{ $device->app_version ?? '—' }}</span></td></tr>
                    <tr><th class="text-muted small">IP</th>
                        <td class="small">{{ $device->ip_address ?? '—' }}</td></tr>
                    <tr><th class="text-muted small">Modelo</th>
                        <td class="small">{{ $device->device_model ?? '—' }}</td></tr>
                    <tr><th class="text-muted small">Sistema</th>
                        <td class="small">Android {{ $device->android_version ?? '—' }}</td></tr>
                    <tr><th class="text-muted small">Registrado</th>
                        <td class="small">{{ $device->created_at->format('d/m/Y H:i') }}</td></tr>
                    <tr><th class="text-muted small">Última vez</th>
                        <td class="small">{{ $device->last_seen_at?->format('d/m/Y H:i') ?? '—' }}</td></tr>
                    <tr><th class="text-muted small">Status</th>
                        <td>
                            @if($device->is_blocked)
                                <span class="badge bg-danger"><i class="bi bi-lock me-1"></i>Bloqueado</span>
                            @else
                                <span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Ativo</span>
                            @endif
                        </td></tr>
                    @if($device->block_reason)
                    <tr><th class="text-muted small">Motivo bloqueio</th>
                        <td class="small text-danger">{{ $device->block_reason }}</td></tr>
                    @endif
                </table>
            </div>
            <div class="card-footer bg-white border-0">
                @if(!$device->is_blocked)
                    <button class="btn btn-sm btn-outline-danger" data-bs-toggle="modal" data-bs-target="#blockModal">
                        <i class="bi bi-lock me-1"></i> Bloquear
                    </button>
                @else
                    <form method="POST" action="{{ route('admin.devices.unblock', $device) }}">
                        @csrf
                        <button class="btn btn-sm btn-outline-success">
                            <i class="bi bi-unlock me-1"></i> Desbloquear
                        </button>
                    </form>
                @endif
            </div>
        </div>
    </div>

    <div class="col-lg-7">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-shield-check me-2 text-success"></i>Licença Ativa
            </div>
            <div class="card-body">
                @if($license)
                    <table class="table table-sm table-borderless mb-0">
                        <tr><th class="text-muted small w-35">Plano</th>
                            <td><span class="badge {{ $license->plan->value === 'pro' ? 'bg-warning text-dark' : 'bg-secondary' }}">{{ strtoupper($license->plan->value) }}</span></td></tr>
                        <tr><th class="text-muted small">Origem</th>
                            <td><span class="badge bg-info text-dark">{{ $license->source->value }}</span></td></tr>
                        <tr><th class="text-muted small">Expira</th>
                            <td class="small">{{ $license->expires_at ? $license->expires_at->format('d/m/Y H:i') : 'Nunca' }}</td></tr>
                        <tr><th class="text-muted small">Criado</th>
                            <td class="small">{{ $license->created_at->format('d/m/Y H:i') }}</td></tr>
                        @if($license->notes)
                        <tr><th class="text-muted small">Notas</th>
                            <td class="small">{{ $license->notes }}</td></tr>
                        @endif
                    </table>
                @else
                    <p class="text-muted mb-0">Nenhuma licença ativa.</p>
                @endif
            </div>
        </div>

        @if($recentReports->isNotEmpty())
        <div class="card shadow-sm mt-3">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-file-text me-2 text-secondary"></i>Últimos Relatórios de Parser
            </div>
            <div class="card-body p-0">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>App</th><th>Campo</th><th>Capturado</th><th>Resultado</th></tr></thead>
                    <tbody>
                    @foreach($recentReports as $report)
                        <tr>
                            <td><span class="badge bg-secondary small">{{ $report->app_key }}</span></td>
                            <td class="small">{{ $report->field_type }}</td>
                            <td class="small text-muted">{{ $report->created_at->diffForHumans() }}</td>
                            <td>
                                @if($report->was_successful)
                                    <span class="badge bg-success">OK</span>
                                @else
                                    <span class="badge bg-danger">Falha</span>
                                @endif
                            </td>
                        </tr>
                    @endforeach
                    </tbody>
                </table>
            </div>
        </div>
        @endif
    </div>
</div>

<!-- Block Modal -->
<div class="modal fade" id="blockModal" tabindex="-1">
    <div class="modal-dialog modal-sm">
        <div class="modal-content">
            <div class="modal-header"><h6 class="modal-title">Bloquear dispositivo</h6><button class="btn-close" data-bs-dismiss="modal"></button></div>
            <form method="POST" action="{{ route('admin.devices.block', $device) }}">
                @csrf
                <div class="modal-body">
                    <textarea name="reason" class="form-control" rows="3" required placeholder="Informe o motivo do bloqueio…"></textarea>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-danger btn-sm"><i class="bi bi-lock me-1"></i> Bloquear</button>
                    <button class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection
