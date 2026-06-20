@extends('admin.layouts.app')
@section('title', 'Usuários')

@section('content')
<div class="d-flex align-items-center mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-people me-2 text-primary"></i>Usuários</h5>
</div>

<div class="card shadow-sm mb-3">
    <div class="card-body py-2">
        <form method="GET" class="row g-2">
            <div class="col-md-6">
                <input type="text" name="search" class="form-control form-control-sm" placeholder="Buscar por email, Google ID…" value="{{ request('search') }}">
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
                <tr><th>Email</th><th>Google ID</th><th>Dispositivos</th><th>Plano</th><th>Criado em</th><th>Ações</th></tr>
            </thead>
            <tbody>
            @forelse($users as $user)
                <tr>
                    <td class="small">{{ $user->email ?? '—' }}</td>
                    <td><code class="small">{{ $user->google_id ? substr($user->google_id, 0, 10).'…' : '—' }}</code></td>
                    <td><span class="badge bg-secondary">{{ $user->devices_count }}</span></td>
                    <td>
                        @php $lic = $user->activeLicense(); @endphp
                        @if($lic && $lic->plan->value === 'pro')
                            <span class="badge bg-warning text-dark">PRO</span>
                        @else
                            <span class="badge bg-light text-dark border">Free</span>
                        @endif
                    </td>
                    <td class="small text-muted">{{ $user->created_at->format('d/m/Y') }}</td>
                    <td>
                        <a href="{{ route('admin.users.show', $user) }}" class="btn btn-sm btn-outline-primary py-0 px-2">
                            <i class="bi bi-eye"></i>
                        </a>
                    </td>
                </tr>
            @empty
                <tr><td colspan="6" class="text-center text-muted py-4">Nenhum usuário encontrado.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
    <div class="card-footer bg-white">{{ $users->withQueryString()->links() }}</div>
</div>
@endsection
