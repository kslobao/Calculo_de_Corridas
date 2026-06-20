<?php

namespace App\Models;

use App\Enums\AdminRole;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Facades\Hash;

class AdminUser extends Model
{
    protected $fillable = [
        'name',
        'email',
        'password',
        'role',
        'is_active',
        'last_login_at',
    ];

    protected $hidden = ['password'];

    protected function casts(): array
    {
        return [
            'role'          => AdminRole::class,
            'is_active'     => 'boolean',
            'last_login_at' => 'datetime',
        ];
    }

    public function setPasswordAttribute(string $value): void
    {
        $this->attributes['password'] = Hash::make($value);
    }

    public function auditLogs(): HasMany
    {
        return $this->hasMany(AuditLog::class);
    }

    public function touchLogin(): void
    {
        $this->update(['last_login_at' => now()]);
    }

    public function canEdit(): bool
    {
        return $this->role->canEdit();
    }

    public function canDelete(): bool
    {
        return $this->role->canDelete();
    }
}
