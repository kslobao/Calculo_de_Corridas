<?php

namespace App\Enums;

enum AppKey: string
{
    case Uber    = 'uber';
    case Nine    = '99';
    case InDrive = 'indrive';
    case IFood   = 'ifood';

    public function displayName(): string
    {
        return match($this) {
            self::Uber    => 'Uber Driver',
            self::Nine    => '99 Motorista',
            self::InDrive => 'inDrive',
            self::IFood   => 'iFood Entregador',
        };
    }

    public function packageName(): string
    {
        return match($this) {
            self::Uber    => 'com.ubercab.driver',
            self::Nine    => 'com.taxis99.driver',
            self::InDrive => 'sinet.startup.inDriver',
            self::IFood   => 'br.com.ifood.driver',
        };
    }

    public function badgeColor(): string
    {
        return match($this) {
            self::Uber    => '#000000',
            self::Nine    => '#FFD700',
            self::InDrive => '#00BCD4',
            self::IFood   => '#EA1D2C',
        };
    }
}
