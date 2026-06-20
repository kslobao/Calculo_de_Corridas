<?php

namespace App\Enums;

enum LicensePlan: string
{
    case Free = 'free';
    case Pro  = 'pro';

    public function label(): string
    {
        return match($this) {
            self::Free => 'Gratuito',
            self::Pro  => 'PRO',
        };
    }

    public function features(): array
    {
        return match($this) {
            self::Free => [
                'ads_free'          => false,
                'unlimited_history' => false,
                'cloud_backup'      => false,
                'export'            => false,
                'multi_vehicle'     => false,
            ],
            self::Pro => [
                'ads_free'          => true,
                'unlimited_history' => true,
                'cloud_backup'      => true,
                'export'            => true,
                'multi_vehicle'     => true,
            ],
        };
    }
}
