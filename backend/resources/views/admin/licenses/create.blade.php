@extends('admin.layouts.app')
@section('title', 'Nova Licença Manual')

@section('content')
<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card shadow-sm">
            <div class="card-header bg-white fw-semibold border-0 pt-3">
                <i class="bi bi-key me-2 text-primary"></i>Nova Licença Manual (PRO)
            </div>
            <div class="card-body">
                <form method="POST" action="{{ route('admin.licenses.store') }}">
                    @csrf
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Device ID <small class="text-muted">(opcional)</small></label>
                        <input type="text" name="device_id" class="form-control" placeholder="UUID do dispositivo" value="{{ old('device_id') }}">
                        <div class="form-text">Se preenchido, a licença fica vinculada ao dispositivo.</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">User ID <small class="text-muted">(opcional)</small></label>
                        <input type="text" name="user_id" class="form-control" placeholder="UUID do usuário" value="{{ old('user_id') }}">
                        <div class="form-text">Se preenchido, a licença vale em todos os dispositivos do usuário.</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Origem *</label>
                        <select name="source" class="form-select" required>
                            <option value="">Selecione…</option>
                            <option value="gift"    {{ old('source') === 'gift'    ? 'selected' : '' }}>🎁 Brinde (Gift)</option>
                            <option value="partner" {{ old('source') === 'partner' ? 'selected' : '' }}>🤝 Parceiro (Partner)</option>
                            <option value="beta"    {{ old('source') === 'beta'    ? 'selected' : '' }}>🧪 Beta Tester</option>
                            <option value="admin"   {{ old('source') === 'admin'   ? 'selected' : '' }}>🔧 Admin</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Expiração <small class="text-muted">(deixe vazio = vitalícia)</small></label>
                        <input type="date" name="expires_at" class="form-control" value="{{ old('expires_at') }}" min="{{ date('Y-m-d', strtotime('+1 day')) }}">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Motivo / Observação</label>
                        <textarea name="reason" class="form-control" rows="2" placeholder="Ex: Influenciador @fulano">{{ old('reason') }}</textarea>
                    </div>
                    <div class="mb-4">
                        <label class="form-label fw-semibold">Notas internas</label>
                        <textarea name="notes" class="form-control" rows="2">{{ old('notes') }}</textarea>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-plus-circle me-1"></i> Criar Licença PRO
                        </button>
                        <a href="{{ route('admin.licenses.index') }}" class="btn btn-outline-secondary">Cancelar</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
@endsection
