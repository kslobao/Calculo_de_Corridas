@extends('admin.layouts.app')
@section('title', 'Novo Seletor')

@section('content')
<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-plus-circle me-2 text-primary"></i>Novo Seletor
            </div>
            <div class="card-body">
                <form method="POST" action="{{ route('admin.selectors.store') }}">
                    @csrf
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Versão *</label>
                            <select name="version_id" class="form-select" required>
                                @foreach($versions as $v)
                                    <option value="{{ $v->id }}" {{ $v->is_active ? 'selected' : '' }}>
                                        v{{ $v->version }}{{ $v->is_active ? ' (ativa)' : '' }}
                                    </option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Aplicativo *</label>
                            <select name="app_key" class="form-select" required>
                                <option value="">Selecione…</option>
                                @foreach(\App\Enums\AppKey::cases() as $app)
                                    <option value="{{ $app->value }}" {{ old('app_key') === $app->value ? 'selected' : '' }}>{{ $app->displayName() }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Campo *</label>
                            <select name="field_type" class="form-select" required>
                                <option value="">Selecione…</option>
                                @foreach(\App\Enums\FieldType::cases() as $ft)
                                    <option value="{{ $ft->value }}" {{ old('field_type') === $ft->value ? 'selected' : '' }}>{{ $ft->label() }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Tipo de seletor *</label>
                            <select name="selector_type" class="form-select" required id="selector-type-select">
                                <option value="">Selecione…</option>
                                @foreach(\App\Enums\SelectorType::cases() as $st)
                                    <option value="{{ $st->value }}" {{ old('selector_type') === $st->value ? 'selected' : '' }}>{{ $st->label() }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Padrão *</label>
                            <input type="text" name="pattern_value" class="form-control font-monospace"
                                   value="{{ old('pattern_value') }}" required
                                   placeholder="Ex: com.ubercab.driver:id/trip_fare">
                            <div class="form-text" id="pattern-hint">Insira o padrão correspondente ao tipo selecionado.</div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Prioridade</label>
                            <input type="number" name="priority" class="form-control" value="{{ old('priority', 10) }}" min="1" max="1000">
                            <div class="form-text">Maior = tenta primeiro.</div>
                        </div>
                        <div class="col-md-8">
                            <label class="form-label fw-semibold">Notas</label>
                            <input type="text" name="notes" class="form-control" value="{{ old('notes') }}" placeholder="Observação interna">
                        </div>
                        <div class="col-12 d-flex gap-2 pt-2">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-check-circle me-1"></i> Salvar Seletor
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

@push('scripts')
<script>
const hints = {
    'ACCESSIBILITY_ID': 'Ex: com.ubercab.driver:id/trip_fare',
    'REGEX':            'Ex: R\\\\$\\\\s*([\\\\d.,]+)',
    'CONTENT_DESC':     'Texto que aparece no contentDescription do elemento',
    'CLASS_NAME':       'Ex: android.widget.TextView'
};
document.getElementById('selector-type-select').addEventListener('change', function() {
    document.getElementById('pattern-hint').textContent = hints[this.value] || '';
});
</script>
@endpush
