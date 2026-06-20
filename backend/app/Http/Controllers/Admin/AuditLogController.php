<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AuditLogController extends Controller
{
    public function index(Request $request): View
    {
        $query = AuditLog::with('adminUser')->latest();

        if ($request->filled('action')) {
            $query->where('action', $request->input('action'));
        }

        if ($request->filled('admin_user_id')) {
            $query->where('admin_user_id', $request->input('admin_user_id'));
        }

        $logs = $query->paginate(30);
        return view('admin.logs.index', compact('logs'));
    }
}
