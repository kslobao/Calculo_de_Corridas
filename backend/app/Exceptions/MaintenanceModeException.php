<?php

namespace App\Exceptions;

use RuntimeException;

class MaintenanceModeException extends RuntimeException
{
    public function __construct(string $message = 'Sistema em manutenção.')
    {
        parent::__construct($message);
    }
}
