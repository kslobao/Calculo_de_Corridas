<?php

namespace App\Enums;

enum SubscriptionStatus: string
{
    case Active       = 'active';
    case Cancelled    = 'cancelled';
    case Expired      = 'expired';
    case Paused       = 'paused';
    case OnHold       = 'on_hold';
    case GracePeriod  = 'grace_period';
    case Revoked      = 'revoked';

    public function isActive(): bool
    {
        return match($this) {
            self::Active, self::GracePeriod => true,
            default                          => false,
        };
    }

    public function label(): string
    {
        return match($this) {
            self::Active      => 'Ativa',
            self::Cancelled   => 'Cancelada',
            self::Expired     => 'Expirada',
            self::Paused      => 'Pausada',
            self::OnHold      => 'Em espera',
            self::GracePeriod => 'Período de graça',
            self::Revoked     => 'Revogada',
        };
    }

    public function badgeClass(): string
    {
        return match($this) {
            self::Active      => 'success',
            self::GracePeriod => 'warning',
            self::Paused      => 'info',
            self::OnHold      => 'secondary',
            default           => 'danger',
        };
    }
}
