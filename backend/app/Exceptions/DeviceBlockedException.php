<?php

namespace App\Exceptions;

use RuntimeException;

class DeviceBlockedException extends RuntimeException
{
    public function __construct(string $reason = 'Device is blocked.')
    {
        parent::__construct($reason);
    }
}
