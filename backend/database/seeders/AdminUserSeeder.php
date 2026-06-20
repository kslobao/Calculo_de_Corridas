<?php

namespace Database\Seeders;

use App\Models\AdminUser;
use Illuminate\Database\Seeder;

class AdminUserSeeder extends Seeder
{
    public function run(): void
    {
        AdminUser::firstOrCreate(
            ['email' => env('ADMIN_DEFAULT_EMAIL', 'admin@calculocorridas.com')],
            [
                'name'     => 'Administrador',
                'password' => env('ADMIN_DEFAULT_PASSWORD', 'change_me_immediately'),
                'role'     => 'admin',
                'is_active'=> true,
            ]
        );
    }
}
