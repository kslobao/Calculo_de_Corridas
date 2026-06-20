<?php

namespace App\Http\Middleware;

use App\Models\AdminUser;
use Closure;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class AdminJwtMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $token = $this->extractToken($request);

        if (!$token) {
            return $this->unauthorized($request);
        }

        try {
            $secret  = config('jwt.secret');
            $decoded = JWT::decode($token, new Key($secret, 'HS256'));
            $adminId = $decoded->sub ?? null;

            if (!$adminId) {
                return $this->unauthorized($request);
            }

            $admin = AdminUser::find($adminId);

            if (!$admin || !$admin->is_active) {
                return $this->unauthorized($request);
            }

            $request->attributes->set('admin_user', $admin);

        } catch (\Throwable) {
            return $this->unauthorized($request);
        }

        return $next($request);
    }

    private function extractToken(Request $request): ?string
    {
        if ($request->hasCookie('admin_token')) {
            return $request->cookie('admin_token');
        }

        $header = $request->header('Authorization', '');
        if (str_starts_with($header, 'Bearer ')) {
            return substr($header, 7);
        }

        return null;
    }

    private function unauthorized(Request $request): Response
    {
        if ($request->expectsJson() || $request->is('api/*')) {
            return response()->json(['message' => 'Unauthenticated.'], 401);
        }

        return redirect()->route('admin.login');
    }
}
