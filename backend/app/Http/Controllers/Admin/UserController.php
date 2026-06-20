<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\View\View;

class UserController extends Controller
{
    public function index(Request $request): View
    {
        $query = User::withTrashed()->latest();

        if ($request->filled('search')) {
            $search = $request->input('search');
            $query->where(fn($q) => $q->where('name', 'ilike', "%{$search}%")
                ->orWhere('email', 'ilike', "%{$search}%"));
        }

        $users = $query->paginate(20);
        return view('admin.users.index', compact('users'));
    }

    public function show(User $user): View
    {
        $user->load(['devices', 'licenses.subscriptions']);
        return view('admin.users.show', compact('user'));
    }
}
