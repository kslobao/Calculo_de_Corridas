<?php

namespace App\Enums;

enum FieldType: string
{
    case Price       = 'price';
    case Distance    = 'distance';
    case Time        = 'time';
    case Origin      = 'origin';
    case Destination = 'destination';
    case Category    = 'category';

    public function label(): string
    {
        return match($this) {
            self::Price       => 'Valor (R$)',
            self::Distance    => 'Distância (km)',
            self::Time        => 'Tempo (min)',
            self::Origin      => 'Origem',
            self::Destination => 'Destino',
            self::Category    => 'Categoria',
        };
    }

    public function androidKey(): string
    {
        return match($this) {
            self::Price       => 'price_patterns',
            self::Distance    => 'distance_patterns',
            self::Time        => 'time_patterns',
            self::Origin      => 'origin_patterns',
            self::Destination => 'destination_patterns',
            self::Category    => 'category_patterns',
        };
    }
}
