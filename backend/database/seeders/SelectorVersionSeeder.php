<?php

namespace Database\Seeders;

use App\Models\Selector;
use App\Models\SelectorVersion;
use Illuminate\Database\Seeder;

class SelectorVersionSeeder extends Seeder
{
    public function run(): void
    {
        if (SelectorVersion::exists()) {
            return;
        }

        $version = SelectorVersion::create([
            'version'     => 1,
            'description' => 'Versão inicial com padrões básicos',
            'is_active'   => true,
            'published_at'=> now(),
        ]);

        $patterns = [
            // Uber — Price
            ['app_key' => 'uber', 'field_type' => 'price', 'selector_type' => 'ACCESSIBILITY_ID', 'pattern_value' => 'com.ubercab.driver:id/trip_fare', 'priority' => 100],
            ['app_key' => 'uber', 'field_type' => 'price', 'selector_type' => 'REGEX', 'pattern_value' => 'R\\$\\s*([\\d.,]+)', 'priority' => 50],
            // Uber — Distance
            ['app_key' => 'uber', 'field_type' => 'distance', 'selector_type' => 'ACCESSIBILITY_ID', 'pattern_value' => 'com.ubercab.driver:id/trip_distance', 'priority' => 100],
            ['app_key' => 'uber', 'field_type' => 'distance', 'selector_type' => 'REGEX', 'pattern_value' => '([\\d.,]+)\\s*km', 'priority' => 50],
            // Uber — Time
            ['app_key' => 'uber', 'field_type' => 'time', 'selector_type' => 'REGEX', 'pattern_value' => '(\\d+)\\s*min', 'priority' => 50],
            // 99 — Price
            ['app_key' => '99', 'field_type' => 'price', 'selector_type' => 'REGEX', 'pattern_value' => 'R\\$\\s*([\\d.,]+)', 'priority' => 50],
            // 99 — Distance
            ['app_key' => '99', 'field_type' => 'distance', 'selector_type' => 'REGEX', 'pattern_value' => '([\\d.,]+)\\s*km', 'priority' => 50],
            // 99 — Time
            ['app_key' => '99', 'field_type' => 'time', 'selector_type' => 'REGEX', 'pattern_value' => '(\\d+)\\s*min', 'priority' => 50],
            // inDrive — Price
            ['app_key' => 'indrive', 'field_type' => 'price', 'selector_type' => 'REGEX', 'pattern_value' => 'R\\$\\s*([\\d.,]+)', 'priority' => 50],
            // inDrive — Distance
            ['app_key' => 'indrive', 'field_type' => 'distance', 'selector_type' => 'REGEX', 'pattern_value' => '([\\d.,]+)\\s*km', 'priority' => 50],
            // inDrive — Time
            ['app_key' => 'indrive', 'field_type' => 'time', 'selector_type' => 'REGEX', 'pattern_value' => '(\\d+)\\s*min', 'priority' => 50],
            // iFood — Price
            ['app_key' => 'ifood', 'field_type' => 'price', 'selector_type' => 'REGEX', 'pattern_value' => 'R\\$\\s*([\\d.,]+)', 'priority' => 50],
            // iFood — Distance
            ['app_key' => 'ifood', 'field_type' => 'distance', 'selector_type' => 'REGEX', 'pattern_value' => '([\\d.,]+)\\s*km', 'priority' => 50],
            // iFood — Time
            ['app_key' => 'ifood', 'field_type' => 'time', 'selector_type' => 'REGEX', 'pattern_value' => '(\\d+)\\s*min', 'priority' => 50],
        ];

        foreach ($patterns as $pattern) {
            Selector::create(array_merge($pattern, ['version_id' => $version->id, 'is_active' => true]));
        }
    }
}
