<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('devices', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignUuid('user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->string('device_token', 64)->unique();
            $table->string('package_name', 100);
            $table->string('app_version', 20)->nullable();
            $table->string('platform', 10)->default('android');
            $table->ipAddress('ip_address')->nullable();
            $table->boolean('is_blocked')->default(false);
            $table->text('blocked_reason')->nullable();
            $table->timestampTz('last_seen_at')->useCurrent();
            $table->timestampsTz();
        });

        DB::statement('CREATE INDEX idx_devices_user_id ON devices (user_id)');
        DB::statement('CREATE INDEX idx_devices_is_blocked ON devices (is_blocked)');
        DB::statement('CREATE INDEX idx_devices_last_seen_at ON devices (last_seen_at DESC)');
        DB::statement('CREATE INDEX idx_devices_app_version ON devices (app_version)');
    }

    public function down(): void
    {
        Schema::dropIfExists('devices');
    }
};
