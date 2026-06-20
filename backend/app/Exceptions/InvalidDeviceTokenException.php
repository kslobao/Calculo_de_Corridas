<?php

namespace App\Exceptions;

use RuntimeException;

class InvalidDeviceTokenException extends RuntimeException
{
    public function __construct()
    {
        parent::__construct('Invalid or missing device token.');
    }
}
