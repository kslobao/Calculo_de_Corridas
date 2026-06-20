<?php

namespace Database\Seeders;

use App\Models\AppConfig;
use Illuminate\Database\Seeder;

class AppConfigSeeder extends Seeder
{
    public function run(): void
    {
        $configs = [
            ['config_key' => 'free_max_rides',          'config_value' => '500',                     'value_type' => 'integer', 'description' => 'Máximo de corridas no plano gratuito', 'is_public' => true],
            ['config_key' => 'free_history_days',       'config_value' => '30',                      'value_type' => 'integer', 'description' => 'Dias de histórico no plano gratuito',  'is_public' => true],
            ['config_key' => 'interstitial_interval',   'config_value' => '5',                       'value_type' => 'integer', 'description' => 'Intervalo de corridas entre interstitials', 'is_public' => true],
            ['config_key' => 'rewarded_enabled',        'config_value' => 'true',                    'value_type' => 'boolean', 'description' => 'Habilitar anúncios remunerados',        'is_public' => true],
            ['config_key' => 'show_promo_banner',       'config_value' => 'false',                   'value_type' => 'boolean', 'description' => 'Exibir banner promocional',             'is_public' => true],
            ['config_key' => 'maintenance_mode',        'config_value' => 'false',                   'value_type' => 'boolean', 'description' => 'Modo manutenção',                      'is_public' => true],
            ['config_key' => 'maintenance_message',     'config_value' => 'Sistema em manutenção.', 'value_type' => 'string',  'description' => 'Mensagem exibida durante manutenção',   'is_public' => true],
            ['config_key' => 'force_update',            'config_value' => 'false',                   'value_type' => 'boolean', 'description' => 'Forçar atualização do app',             'is_public' => true],
            ['config_key' => 'minimum_version',         'config_value' => '1',                       'value_type' => 'integer', 'description' => 'Versão mínima do app (versionCode)',    'is_public' => true],
            ['config_key' => 'latest_version',          'config_value' => '1',                       'value_type' => 'integer', 'description' => 'Versão mais recente do app',            'is_public' => true],
            ['config_key' => 'beta_ocr_enabled',        'config_value' => 'false',                   'value_type' => 'boolean', 'description' => 'Habilitar OCR em beta',                 'is_public' => true],
            ['config_key' => 'google_package_name',     'config_value' => 'com.calculocorridas',     'value_type' => 'string',  'description' => 'Package name do app Android',           'is_public' => false],
        ];

        foreach ($configs as $config) {
            AppConfig::firstOrCreate(['config_key' => $config['config_key']], $config);
        }
    }
}
