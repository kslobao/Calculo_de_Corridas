@extends('admin.layouts.app')
@section('title', 'Editar Licença')

@section('content')
<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3 d-flex justify-content-between">
                <span><i class="bi bi-pencil me-2 text-primary"></i>Editar Licença</span>
                <code class="small text-muted">{{ substr($license->id, 0, 8) }}…</code>
            </div>
            <div class="card-body">
                <form method="POST" action="{{ route('admin.licenses.update', $license) }}">
                    @csrf @method('PUT')
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Plano *</label>
                            <select name="plan" class="form-select" required>
                                @foreach(\App\Enums\LicensePlan::cases() as $plan)
                                    <option value="{{ $plan->value }}" {{ $license->plan->value === $plan->value ? 'selected' : '' }}>
                                        {{ strtoupper($plan->value) }}
                                    </option>
                                @endforeach
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Origem *</label>
                            <select name="source" class="form-select" required>
                                @foreach(\App\Enums\LicenseSource::cases() as $src)
                                    @if($src->value !== 'google')
                                    <option value="{{ $src->value }}" {{ $license->source->value === $src->value ? 'selected' : '' }}>
                                        {{ match($src->value) {
                                            'free'    => '⚪ free',
                                            'gift'    => '🎁 gift',
                                            'partner' => '🤝 partner',
                                            'beta'    => '🧪 beta',
                                            'admin'   => '🔑 admin',
                                            default   => $src->value,
                                        } }}
                                    </option>
                                    @endif
                                @endforeach
                            </select>
                        </div>

                        <div class="col-12">
                            <label class="form-label fw-semibold">Device ID</label>
                            <input type="text" name="device_id" class="form-control font-monospace"
                                   value="{{ old('device_id', $license->device_id) }}" placeholder="UUID do dispositivo (opcional)">
                            <div class="form-text">Deixe vazio para licença de usuário (todos os dispositivos).</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">User ID</label>
                            <input type="text" name="user_id" class="form-control font-monospace"
                                   value="{{ old('user_id', $license->user_id) }}" placeholder="UUID do usuário (opcional)">
                        </div>

                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Expira em</label>
                            <input type="datetime-local" name="expires_at" class="form-control"
                                   value="{{ old('expires_at', $license->expires_at?->format('Y-m-d\TH:i')) }}">
                            <div class="form-text">Deixe vazio para nunca expirar.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Ativa</label>
                            <div class="form-check form-switch mt-2">
                                <input class="form-check-input" type="checkbox" name="is_active" value="1" {{ $license->is_active ? 'checked' : '' }}>
                                <label class="form-check-label">Habilitada</label>
                            </div>
                        </div>

                        <div class="col-12">
                            <label class="form-label fw-semibold">Motivo / Razão</label>
                            <input type="text" name="reason" class="form-control"
                                   value="{{ old('reason', $license->reason) }}" placeholder="Ex: Parceiro de divulgação">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Notas internas</label>
                            <textarea name="notes" class="form-control" rows="2"
                                      placeholder="Visível somente no admin">{{ old('notes', $license->notes) }}</textarea>
                        </div>

                        <div class="col-12 d-flex gap-2 pt-2">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-check-circle me-1"></i> Salvar
                            </button>
                            <a href="{{ route('admin.licenses.show', $license) }}" class="btn btn-outline-secondary">Cancelar</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
@endsection
