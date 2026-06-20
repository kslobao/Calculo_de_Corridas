@extends('admin.layouts.app')
@section('title', 'Assinaturas')

@section('content')
<div class="d-flex align-items-center mb-3">
    <h5 class="mb-0 fw-bold"><i class="bi bi-credit-card me-2 text-warning"></i>Assinaturas Google Play</h5>
</div>

<div class="card shadow-sm mb-3">
    <div class="card-body py-2">
        <form method="GET" class="row g-2">
            <div class="col-md-4">
                <input type="text" name="search" class="form-control form-control-sm" placeholder="Token, produto…" value="{{ request('search') }}">
            </div>
            <div class="col-md-3">
                <select name="status" class="form-select form-select-sm">
                    <option value="">Todos os status</option>
                    @foreach(\App\Enums\SubscriptionStatus::cases() as $s)
                        <option value="{{ $s->value }}" {{ request('status') === $s->value ? 'selected' : '' }}>{{ $s->value }}</option>
                    @endforeach
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
                <tr><th>Purchase Token (início)</th><th>Produto</th><th>Status</th><th>Expira</th><th>Atualizado</th><th>Ações</th></tr>
            </thead>
            <tbody>
            @forelse($subscriptions as $sub)
                <tr>
                    <td><code class="small">{{ substr($sub->purchase_token, 0, 16) }}…</code></td>
                    <td><span class="badge bg-light text-dark border small">{{ $sub->product_id }}</span></td>
                    <td>
                        @php $active = $sub->status->isActive(); @endphp
                        <span class="badge {{ $active ? 'bg-success' : 'bg-secondary' }}">{{ $sub->status->value }}</span>
                    </td>
                    <td class="small {{ $sub->expires_at?->isPast() ? 'text-danger' : '' }}">
                        {{ $sub->expires_at?->format('d/m/Y H:i') ?? '—' }}
                    </td>
                    <td class="small text-muted">{{ $sub->updated_at->diffForHumans() }}</td>
                    <td>
                        <form method="POST" action="{{ route('admin.subscriptions.revalidate', $sub) }}">
                            @csrf
                            <button class="btn btn-sm btn-outline-warning py-0 px-2" title="Revalidar no Google Play">
                                <i class="bi bi-arrow-clockwise"></i>
                            </button>
                        </form>
                    </td>
                </tr>
            @empty
                <tr><td colspan="6" class="text-center text-muted py-4">Nenhuma assinatura encontrada.</td></tr>
            @endforelse
            </tbody>
        </table>
    </div>
    <div class="card-footer bg-white">{{ $subscriptions->withQueryString()->links() }}</div>
</div>
@endsection
