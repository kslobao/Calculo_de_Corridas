<?php

return [
    'secret'  => env('JWT_SECRET'),
    'algo'    => env('JWT_ALGO', 'HS256'),
    'ttl'     => env('JWT_TTL', 1440),
];
