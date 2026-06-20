<?php

namespace App\Enums;

enum LicenseSource: string
{
    case Free    = 'free';
    case Google  = 'google';
    case Gift    = 'gift';
    case Partner = 'partner';
    case Beta    = 'beta';
    case Admin   = 'admin';

    public function label(): string
    {
        return match($this) {
            self::Free    => 'Gratuito',
            self::Google  => 'Google Play',
            self::Gift    => 'Brinde',
            self::Partner => 'Parceiro',
            self::Beta    => 'Beta Tester',
            self::Admin   => 'Administrador',
        };
    }

    public function isManual(): bool
    {
        return $this !== self::Google && $this !== self::Free;
    }

    public function badgeClass(): string
    {
        return match($this) {
            self::Free    => 'secondary',
            self::Google  => 'primary',
            self::Gift    => 'success',
            self::Partner => 'info',
            self::Beta    => 'warning',
            self::Admin   => 'danger',
        };
    }
}
