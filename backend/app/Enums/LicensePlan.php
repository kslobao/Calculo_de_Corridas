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
                'advanced_rules'    => false,
                'export_enabled'    => false,
                'history_unlimited' => false,
                'max_rides'         => 500,
                'history_days'      => 30,
            ],
            self::Pro => [
                'ads_free'          => true,
                'advanced_rules'    => true,
                'export_enabled'    => true,
                'history_unlimited' => true,
                'max_rides'         => -1,
                'history_days'      => -1,
            ],
        };
    }
}
