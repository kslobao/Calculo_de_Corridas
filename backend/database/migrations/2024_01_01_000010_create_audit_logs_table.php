<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('audit_logs', function (Blueprint $table) {
            $table->bigIncrements('id');
            $table->unsignedInteger('admin_user_id')->nullable();
            $table->foreign('admin_user_id')->references('id')->on('admin_users')->nullOnDelete();
            $table->string('action', 100);
            $table->string('model_type', 100)->nullable();
            $table->string('model_id', 100)->nullable();
            $table->jsonb('old_values')->nullable();
            $table->jsonb('new_values')->nullable();
            $table->ipAddress('ip_address')->nullable();
            $table->text('user_agent')->nullable();
            $table->timestampTz('created_at')->useCurrent();
        });

        DB::statement('CREATE INDEX idx_audit_logs_admin_user_id ON audit_logs (admin_user_id)');
        DB::statement('CREATE INDEX idx_audit_logs_action ON audit_logs (action)');
        DB::statement('CREATE INDEX idx_audit_logs_model ON audit_logs (model_type, model_id)');
        DB::statement('CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC)');
    }

    public function down(): void
    {
        Schema::dropIfExists('audit_logs');
    }
};
