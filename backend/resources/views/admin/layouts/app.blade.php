<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>@yield('title', 'Admin') — Cálculo de Corridas</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="https://cdn.datatables.net/1.13.8/css/dataTables.bootstrap5.min.css">
    <style>
        body { background: #f4f6f9; }
        .sidebar { min-height: 100vh; background: #1a1d23; width: 240px; position: fixed; top: 0; left: 0; z-index: 100; padding: 0; }
        .sidebar-brand { padding: 1.25rem 1.5rem; color: #fff; font-size: 1rem; font-weight: 700; border-bottom: 1px solid rgba(255,255,255,.1); display: flex; align-items: center; gap: .5rem; }
        .sidebar .nav-link { color: rgba(255,255,255,.65); padding: .6rem 1.5rem; border-radius: 0; font-size: .875rem; display: flex; align-items: center; gap: .6rem; transition: all .15s; }
        .sidebar .nav-link:hover, .sidebar .nav-link.active { color: #fff; background: rgba(255,255,255,.08); }
        .sidebar .nav-section { padding: .75rem 1.5rem .25rem; text-transform: uppercase; font-size: .7rem; color: rgba(255,255,255,.35); letter-spacing: .08em; }
        .main-content { margin-left: 240px; min-height: 100vh; }
        .topbar { background: #fff; border-bottom: 1px solid #e9ecef; padding: .75rem 1.5rem; display: flex; align-items: center; justify-content: space-between; }
        .page-header { background: #fff; border-bottom: 1px solid #e9ecef; padding: 1rem 1.5rem; margin-bottom: 1.5rem; }
        .content-area { padding: 0 1.5rem 2rem; }
        .stat-card { border: none; border-radius: .75rem; }
        .stat-card .card-body { padding: 1.25rem; }
        .stat-icon { width: 48px; height: 48px; border-radius: .5rem; display: flex; align-items: center; justify-content: center; font-size: 1.4rem; }
        .badge-source-free    { background: #6c757d; }
        .badge-source-google  { background: #0d6efd; }
        .badge-source-gift    { background: #198754; }
        .badge-source-partner { background: #0dcaf0; color: #000; }
        .badge-source-beta    { background: #ffc107; color: #000; }
        .badge-source-admin   { background: #dc3545; }
        @media (max-width: 768px) { .sidebar { display: none; } .main-content { margin-left: 0; } }
    </style>
    @stack('styles')
</head>
<body>

<div class="sidebar">
    <div class="sidebar-brand">
        <i class="bi bi-speedometer2 text-primary"></i> Corridas Admin
    </div>
    <ul class="nav flex-column mt-2">
        <li><span class="nav-section">Principal</span></li>
        <li class="nav-item">
            <a href="{{ route('admin.dashboard') }}" class="nav-link {{ request()->routeIs('admin.dashboard') ? 'active' : '' }}">
                <i class="bi bi-grid-1x2"></i> Dashboard
            </a>
        </li>
        <li><span class="nav-section">Usuários</span></li>
        <li class="nav-item">
            <a href="{{ route('admin.users.index') }}" class="nav-link {{ request()->routeIs('admin.users.*') ? 'active' : '' }}">
                <i class="bi bi-people"></i> Usuários
            </a>
        </li>
        <li class="nav-item">
            <a href="{{ route('admin.devices.index') }}" class="nav-link {{ request()->routeIs('admin.devices.*') ? 'active' : '' }}">
                <i class="bi bi-phone"></i> Dispositivos
            </a>
        </li>
        <li><span class="nav-section">Monetização</span></li>
        <li class="nav-item">
            <a href="{{ route('admin.licenses.index') }}" class="nav-link {{ request()->routeIs('admin.licenses.*') ? 'active' : '' }}">
                <i class="bi bi-key"></i> Licenças
            </a>
        </li>
        <li class="nav-item">
            <a href="{{ route('admin.subscriptions.index') }}" class="nav-link {{ request()->routeIs('admin.subscriptions.*') ? 'active' : '' }}">
                <i class="bi bi-credit-card-2-front"></i> Assinaturas
            </a>
        </li>
        <li><span class="nav-section">Sistema</span></li>
        <li class="nav-item">
            <a href="{{ route('admin.selectors.index') }}" class="nav-link {{ request()->routeIs('admin.selectors.*') ? 'active' : '' }}">
                <i class="bi bi-code-slash"></i> Seletores
            </a>
        </li>
        <li class="nav-item">
            <a href="{{ route('admin.config.index') }}" class="nav-link {{ request()->routeIs('admin.config.*') ? 'active' : '' }}">
                <i class="bi bi-gear"></i> Remote Config
            </a>
        </li>
        <li class="nav-item">
            <a href="{{ route('admin.reports.index') }}" class="nav-link {{ request()->routeIs('admin.reports.*') ? 'active' : '' }}">
                <i class="bi bi-exclamation-triangle"></i> Relatórios
            </a>
        </li>
        <li class="nav-item">
            <a href="{{ route('admin.logs.index') }}" class="nav-link {{ request()->routeIs('admin.logs.*') ? 'active' : '' }}">
                <i class="bi bi-list-ul"></i> Logs
            </a>
        </li>
    </ul>
    <div class="mt-auto p-3" style="position:absolute;bottom:0;width:100%;">
        <form method="POST" action="{{ route('admin.logout') }}">
            @csrf
            <button class="btn btn-sm btn-outline-secondary w-100 text-white border-secondary">
                <i class="bi bi-box-arrow-right"></i> Sair
            </button>
        </form>
    </div>
</div>

<div class="main-content">
    <div class="topbar">
        <div class="fw-semibold text-dark">@yield('title', 'Dashboard')</div>
        <div class="text-muted small"><i class="bi bi-person-circle me-1"></i>Admin</div>
    </div>

    <div class="content-area mt-3">
        @if(session('success'))
            <div class="alert alert-success alert-dismissible fade show shadow-sm">
                <i class="bi bi-check-circle me-2"></i>{{ session('success') }}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        @endif
        @if(session('error'))
            <div class="alert alert-danger alert-dismissible fade show shadow-sm">
                <i class="bi bi-x-circle me-2"></i>{{ session('error') }}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        @endif
        @if($errors->any())
            <div class="alert alert-danger shadow-sm">
                <ul class="mb-0">@foreach($errors->all() as $e)<li>{{ $e }}</li>@endforeach</ul>
            </div>
        @endif
        @yield('content')
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
<script src="https://cdn.datatables.net/1.13.8/js/jquery.dataTables.min.js"></script>
<script src="https://cdn.datatables.net/1.13.8/js/dataTables.bootstrap5.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script>
    $.ajaxSetup({ headers: { 'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content') } });
    $('[data-bs-toggle="tooltip"]').tooltip();
</script>
@stack('scripts')
</body>
</html>
