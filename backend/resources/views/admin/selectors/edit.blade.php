@extends('admin.layouts.app')
@section('title', 'Editar Seletor')

@section('content')
<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3 d-flex justify-content-between">
                <span><i class="bi bi-pencil me-2 text-primary"></i>Editar Seletor</span>
                <code class="small text-muted">{{ substr($selector->id, 0, 8) }}…</code>
            </div>
            <div class="card-body">
                <form method="POST" action="{{ route('admin.selectors.update', $selector) }}">
                    @csrf @method('PUT')
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Versão *</label>
                            <select name="version_id" class="form-select" required>
                                @foreach($versions as $v)
                                    <option value="{{ $v->id }}" {{ $selector->version_id == $v->id ? 'selected' : '' }}>
                                        v{{ $v->version }}{{ $v->is_active ? ' (ativa)' : '' }}
                                    </option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Aplicativo *</label>
                            <select name="app_key" class="form-select" required>
                                @foreach(\App\Enums\AppKey::cases() as $app)
                                    <option value="{{ $app->value }}" {{ $selector->app_key->value === $app->value ? 'selected' : '' }}>{{ $app->displayName() }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Campo *</label>
                            <select name="field_type" class="form-select" required>
                                @foreach(\App\Enums\FieldType::cases() as $ft)
                                    <option value="{{ $ft->value }}" {{ $selector->field_type->value === $ft->value ? 'selected' : '' }}>{{ $ft->label() }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Tipo de seletor *</label>
                            <select name="selector_type" class="form-select" required>
                                @foreach(\App\Enums\SelectorType::cases() as $st)
                                    <option value="{{ $st->value }}" {{ $selector->selector_type->value === $st->value ? 'selected' : '' }}>{{ $st->label() }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Padrão *</label>
                            <input type="text" name="pattern_value" class="form-control font-monospace"
                                   value="{{ old('pattern_value', $selector->pattern_value) }}" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Prioridade</label>
                            <input type="number" name="priority" class="form-control" value="{{ old('priority', $selector->priority) }}" min="1" max="1000">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Ativo</label>
                            <div class="form-check form-switch mt-2">
                                <input class="form-check-input" type="checkbox" name="is_active" value="1" {{ $selector->is_active ? 'checked' : '' }}>
                                <label class="form-check-label">Habilitado</label>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Notas</label>
                            <input type="text" name="notes" class="form-control" value="{{ old('notes', $selector->notes) }}">
                        </div>
                        <div class="col-12 d-flex gap-2 pt-2">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-check-circle me-1"></i> Salvar
                            </button>
                            <a href="{{ route('admin.selectors.index') }}" class="btn btn-outline-secondary">Cancelar</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
@endsection
