@extends('admin.layouts.app')
@section('title', 'Remote Config')

@section('content')
<div class="d-flex align-items-center mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-gear me-2 text-primary"></i>Remote Config</h5>
</div>

<div class="card shadow-sm">
    <div class="card-body p-0">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Chave</th><th>Tipo</th><th>Valor</th><th>Público</th><th>Ações</th></tr>
            </thead>
            <tbody>
            @foreach($configs as $config)
                <tr>
                    <td><code class="small">{{ $config->config_key }}</code>
                        @if($config->description)
                            <div class="text-muted" style="font-size:.75rem">{{ $config->description }}</div>
                        @endif
                    </td>
                    <td><span class="badge bg-light text-dark border">{{ $config->value_type }}</span></td>
                    <td>
                        <span class="font-monospace small">{{ Str::limit($config->config_value, 50) }}</span>
                    </td>
                    <td>{{ $config->is_public ? '✅' : '🔒' }}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary py-0 px-2"
                                data-bs-toggle="modal" data-bs-target="#editModal{{ $loop->index }}"
                                title="Editar">
                            <i class="bi bi-pencil"></i>
                        </button>
                    </td>
                </tr>
            @endforeach
            </tbody>
        </table>
    </div>
</div>

@foreach($configs as $i => $config)
<div class="modal fade" id="editModal{{ $i }}" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Editar: <code>{{ $config->config_key }}</code></h5>
                <button class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form method="POST" action="{{ route('admin.config.update', $config->config_key) }}">
                @csrf @method('PUT')
                <div class="modal-body">
                    <div class="mb-2 text-muted small">{{ $config->description }}</div>
                    <label class="form-label fw-semibold">Valor (tipo: {{ $config->value_type }})</label>
                    @if($config->value_type === 'boolean')
                        <select name="config_value" class="form-select">
                            <option value="true"  {{ $config->config_value === 'true'  ? 'selected' : '' }}>true</option>
                            <option value="false" {{ $config->config_value === 'false' ? 'selected' : '' }}>false</option>
                        </select>
                    @else
                        <input type="{{ in_array($config->value_type, ['integer','float']) ? 'number' : 'text' }}"
                               name="config_value" class="form-control font-monospace"
                               value="{{ $config->config_value }}" required>
                    @endif
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary">Salvar</button>
                    <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                </div>
            </form>
        </div>
    </div>
</div>
@endforeach
@endsection
