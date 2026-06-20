<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\LoginRequest;
use App\Models\AdminUser;
use Firebase\JWT\JWT;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cookie;
use Illuminate\Support\Facades\Hash;
use Illuminate\View\View;

class AuthController extends Controller
{
    public function showLogin(): View
    {
        return view('admin.auth.login');
    }

    public function login(LoginRequest $request): RedirectResponse
    {
        $admin = AdminUser::where('email', $request->input('email'))
            ->where('is_active', true)
            ->first();

        if (!$admin || !Hash::check($request->input('password'), $admin->password)) {
            return back()->withErrors(['email' => 'Credenciais inválidas.'])->withInput();
        }

        $admin->touchLogin();

        $now   = time();
        $ttl   = (int) config('jwt.ttl', 1440) * 60;
        $token = JWT::encode([
            'sub' => $admin->id,
            'iat' => $now,
            'exp' => $now + $ttl,
        ], config('jwt.secret'), 'HS256');

        $cookie = Cookie::make('admin_token', $token, config('jwt.ttl', 1440), '/', null, true, true, false, 'Strict');

        return redirect()->route('admin.dashboard')->withCookie($cookie);
    }

    public function logout(Request $request): RedirectResponse
    {
        $cookie = Cookie::forget('admin_token');
        return redirect()->route('admin.login')->withCookie($cookie);
    }
}
