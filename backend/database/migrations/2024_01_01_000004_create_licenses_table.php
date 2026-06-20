<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('licenses', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignUuid('user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->foreignUuid('device_id')->nullable()->constrained('devices')->nullOnDelete();
            $table->string('plan', 20)->default('free');
            $table->string('source', 20)->default('free');
            $table->boolean('is_active')->default(true);
            $table->timestampTz('expires_at')->nullable();
            $table->string('purchase_token', 500)->nullable();
            $table->string('product_id', 100)->nullable();
            $table->text('reason')->nullable();
            $table->text('notes')->nullable();
            $table->unsignedInteger('created_by')->nullable();
            $table->foreign('created_by')->references('id')->on('admin_users')->nullOnDelete();
            $table->timestampsTz();
            $table->softDeletesTz();
        });

        DB::statement("ALTER TABLE licenses ADD CONSTRAINT chk_licenses_plan CHECK (plan IN ('free','pro'))");
        DB::statement("ALTER TABLE licenses ADD CONSTRAINT chk_licenses_source CHECK (source IN ('free','google','gift','partner','beta','admin'))");
        DB::statement('CREATE INDEX idx_licenses_user_id ON licenses (user_id)');
        DB::statement('CREATE INDEX idx_licenses_device_id ON licenses (device_id)');
        DB::statement('CREATE INDEX idx_licenses_source ON licenses (source)');
        DB::statement('CREATE INDEX idx_licenses_is_active ON licenses (is_active)');
        DB::statement('CREATE INDEX idx_licenses_expires_at ON licenses (expires_at)');
        DB::statement('CREATE INDEX idx_licenses_deleted_at ON licenses (deleted_at)');
    }

    public function down(): void
    {
        Schema::dropIfExists('licenses');
    }
};
