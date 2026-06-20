@extends('admin.layouts.app')
@section('title', 'Relatórios de Parser')

@section('content')
<div class="d-flex align-items-center mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-file-text me-2 text-secondary"></i>Relatórios de Parser</h5>
</div>

<!-- 7-day failure summary -->
<div class="row g-3 mb-3">
    @foreach($failureStats as $stat)
    <div class="col-md-3">
        <div class="card shadow-sm border-0">
            <div class="card-body py-2">
                <div class="d-flex align-items-center gap-2">
                    <span class="badge" style="background:{{ \App\Enums\AppKey::from($stat->app_key)->badgeColor() }}; color:#fff; font-size:.85rem">
                        {{ $stat->app_key }}
                    </span>
                    <div>
                        <div class="fw-bold text-danger">{{ $stat->failures }} falhas</div>
                        <div class="text-muted" style="font-size:.75rem">últimos 7 dias · {{ $stat->field_type }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    @endforeach
</div>

<div class="card shadow-sm mb-3">
    <div class="card-body py-2">
        <form method="GET" class="row g-2">
            <div class="col-md-3">
                <select name="app_key" class="form-select form-select-sm">
                    <option value="">Todos os apps</option>
                    @foreach(\App\Enums\AppKey::cases() as $app)
                        <option value="{{ $app->value }}" {{ request('app_key') === $app->value ? 'selected' : '' }}>{{ $app->displayName() }}</option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-3">
                <select name="was_successful" class="form-select form-select-sm">
                    <option value="">Todos</option>
                    <option value="1" {{ request('was_successful') === '1' ? 'selected' : '' }}>Bem-sucedidos</option>
                    <option value="0" {{ request('was_successful') === '0' ? 'selected' : '' }}>Falhas</option>
                </select>
            </div>
            <div class="col-md-3">
                <select name="field_type" class="form-select form-select-sm">
                    <option value="">Todos os campos</option>
                    @foreach(\App\Enums\FieldType::cases() as $ft)
                        <option value="{{ $ft->value }}" {{ request('field_type') === $ft->value ? 'selected' : '' }}>{{ $ft->label() }}</option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-1">
                <button class="btn btn-sm btn-outline-primary w-100">Filtrar</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow-sm">
    <div class="card-body p-0">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>App</th><th>Campo</th><th>Resultado</th><th>Textos capturados</th><th>Seletor usado</th><th>Data</th></tr>
            </thead>
            <tbody>
            @forelse($reports as $report)
                <tr class="{{ !$report->was_successful ? 'table-warning' : '' }}">
                    <td>
                        <span class="badge" style="background:{{ \App\Enums\AppKey::from($report->app_key)->badgeColor() }}; color:#fff">
                            {{ $report->app_key }}
                        </span>
                    </td>
                    <td class="small">{{ $report->field_type }}</td>
                    <td>
                        @if($report->was_successful)
                            <span class="badge bg-success"><i class="bi bi-check me-1"></i>OK</span>
                        @else
                            <span class="badge bg-danger"><i class="bi bi-x me-1"></i>Falha</span>
                        @endif
                    </td>
                    <td>
                        <button class="btn btn-xs btn-outline-secondary btn-sm py-0 px-2"
                                data-bs-toggle="collapse" data-bs-target="#texts{{ $loop->index }}">
                            <i class="bi bi-eye"></i>
                        </button>
                        <div class="collapse mt-1" id="texts{{ $loop->index }}">
                            @foreach(($report->raw_texts ?? []) as $txt)
                                <code class="small d-block text-break">{{ $txt }}</code>
                            @endforeach
                        </div>
                    </td>
                    <td><code class="small">{{ $report->selector_id ? substr($report->selector_id, 0, 8).'…' : '—' }}</code></td>
                    <td class="small text-muted">{{ $report->created_at->format('d/m/Y H:i') }}</td>
                </tr>
            @empty
                <tr><td colspan="6" class="text-center text-muted py-4">Nenhum relatório encontrado.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
    <div class="card-footer bg-white">{{ $reports->withQueryString()->links() }}</div>
</div>
@endsection
