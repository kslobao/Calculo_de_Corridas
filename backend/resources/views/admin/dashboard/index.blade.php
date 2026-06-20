@extends('admin.layouts.app')
@section('title', 'Dashboard')

@section('content')
<div class="row g-3 mb-4" id="stat-cards">
    <div class="col-6 col-md-3">
        <div class="card stat-card shadow-sm">
            <div class="card-body d-flex align-items-center gap-3">
                <div class="stat-icon bg-primary bg-opacity-10 text-primary"><i class="bi bi-people"></i></div>
                <div>
                    <div class="fs-4 fw-bold" id="stat-users">—</div>
                    <div class="text-muted small">Usuários</div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-6 col-md-3">
        <div class="card stat-card shadow-sm">
            <div class="card-body d-flex align-items-center gap-3">
                <div class="stat-icon bg-info bg-opacity-10 text-info"><i class="bi bi-phone"></i></div>
                <div>
                    <div class="fs-4 fw-bold" id="stat-devices">—</div>
                    <div class="text-muted small">Dispositivos</div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-6 col-md-3">
        <div class="card stat-card shadow-sm">
            <div class="card-body d-flex align-items-center gap-3">
                <div class="stat-icon bg-warning bg-opacity-10 text-warning"><i class="bi bi-key"></i></div>
                <div>
                    <div class="fs-4 fw-bold" id="stat-pro">—</div>
                    <div class="text-muted small">Licenças PRO</div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-6 col-md-3">
        <div class="card stat-card shadow-sm">
            <div class="card-body d-flex align-items-center gap-3">
                <div class="stat-icon bg-success bg-opacity-10 text-success"><i class="bi bi-credit-card"></i></div>
                <div>
                    <div class="fs-4 fw-bold" id="stat-subs">—</div>
                    <div class="text-muted small">Assinaturas ativas</div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-3 mb-4">
    <div class="col-md-8">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-phone text-primary me-1"></i> Dispositivos ativos — últimos 30 dias
            </div>
            <div class="card-body"><canvas id="devicesChart" height="100"></canvas></div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-exclamation-triangle text-danger me-1"></i> Falhas de parsing (7d)
            </div>
            <div class="card-body"><canvas id="failuresChart" height="160"></canvas></div>
        </div>
    </div>
</div>

<div class="card shadow-sm">
    <div class="card-header bg-white fw-semibold border-0 pt-3">
        <i class="bi bi-clock-history text-muted me-1"></i> Últimos dispositivos conectados
    </div>
    <div class="card-body p-0">
        <table class="table table-hover mb-0" id="devices-table">
            <thead class="table-light">
                <tr><th>Versão</th><th>IP</th><th>Última vez</th><th>Bloqueado</th></tr>
            </thead>
            <tbody id="latest-devices-body">
                <tr><td colspan="4" class="text-center text-muted py-3">Carregando...</td></tr>
            </tbody>
        </table>
    </div>
</div>
@endsection

@push('scripts')
<script>
fetch('{{ route("admin.stats") }}')
    .then(r => r.json())
    .then(data => {
        document.getElementById('stat-users').textContent   = data.totals.users.toLocaleString('pt-BR');
        document.getElementById('stat-devices').textContent = data.totals.devices.toLocaleString('pt-BR');
        document.getElementById('stat-pro').textContent     = data.totals.pro_licenses.toLocaleString('pt-BR');
        document.getElementById('stat-subs').textContent    = data.totals.active_subscriptions.toLocaleString('pt-BR');

        const labels  = Object.keys(data.devices_last_30_days);
        const values  = Object.values(data.devices_last_30_days);
        new Chart(document.getElementById('devicesChart'), {
            type: 'line',
            data: { labels, datasets: [{ label: 'Dispositivos', data: values, borderColor: '#0d6efd', backgroundColor: 'rgba(13,110,253,.1)', tension: .3, fill: true }] },
            options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
        });

        const appLabels  = Object.keys(data.failures_by_app);
        const appValues  = Object.values(data.failures_by_app);
        new Chart(document.getElementById('failuresChart'), {
            type: 'doughnut',
            data: { labels: appLabels, datasets: [{ data: appValues, backgroundColor: ['#000','#ffc107','#00bcd4','#ea1d2c'] }] },
            options: { plugins: { legend: { position: 'bottom' } } }
        });

        const tbody = document.getElementById('latest-devices-body');
        tbody.innerHTML = data.latest_devices.map(d => `
            <tr>
                <td><span class="badge bg-secondary">${d.app_version ?? '?'}</span></td>
                <td class="text-muted small">${d.ip_address ?? '—'}</td>
                <td class="small">${d.last_seen_at}</td>
                <td>${d.is_blocked ? '<span class="badge bg-danger">Sim</span>' : '<span class="badge bg-success">Não</span>'}</td>
            </tr>`).join('');
    });
</script>
@endpush
