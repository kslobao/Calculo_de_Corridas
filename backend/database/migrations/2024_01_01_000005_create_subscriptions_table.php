<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('subscriptions', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignUuid('license_id')->constrained('licenses')->cascadeOnDelete();
            $table->string('product_id', 100);
            $table->string('purchase_token', 500)->unique();
            $table->string('google_order_id', 100)->nullable();
            $table->string('status', 30)->default('active');
            $table->timestampTz('started_at')->nullable();
            $table->timestampTz('expires_at')->nullable();
            $table->timestampTz('cancelled_at')->nullable();
            $table->timestampTz('last_validated_at')->nullable();
            $table->jsonb('raw_google_response')->nullable();
            $table->timestampsTz();
        });

        DB::statement("ALTER TABLE subscriptions ADD CONSTRAINT chk_subscriptions_status CHECK (status IN ('active','cancelled','expired','paused','on_hold','grace_period','revoked'))");
        DB::statement('CREATE INDEX idx_subscriptions_license_id ON subscriptions (license_id)');
        DB::statement('CREATE INDEX idx_subscriptions_status ON subscriptions (status)');
        DB::statement('CREATE INDEX idx_subscriptions_expires_at ON subscriptions (expires_at)');
        DB::statement('CREATE INDEX idx_subscriptions_product_id ON subscriptions (product_id)');
    }

    public function down(): void
    {
        Schema::dropIfExists('subscriptions');
    }
};
