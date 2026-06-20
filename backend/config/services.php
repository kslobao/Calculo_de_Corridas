<?php

return [
    'google_play' => [
        'package_name'         => env('GOOGLE_PLAY_PACKAGE_NAME', 'com.calculocorridas'),
        'service_account_json' => env('GOOGLE_SERVICE_ACCOUNT_JSON', '{}'),
        'rtdn_token'           => env('RTDN_PUBSUB_TOKEN', ''),
        'default_product_id'   => env('GOOGLE_PLAY_DEFAULT_PRODUCT_ID', 'pro_monthly'),
    ],
];
