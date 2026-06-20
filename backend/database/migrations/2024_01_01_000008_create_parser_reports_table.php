<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('parser_reports', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignUuid('device_id')->nullable()->constrained('devices')->nullOnDelete();
            $table->string('app_key', 20);
            $table->unsignedInteger('selector_version')->nullable();
            $table->jsonb('raw_texts')->nullable();
            $table->decimal('parsed_value', 10, 2)->nullable();
            $table->decimal('parsed_distance', 10, 2)->nullable();
            $table->unsignedInteger('parsed_duration_min')->nullable();
            $table->boolean('success')->default(false);
            $table->text('error_message')->nullable();
            $table->string('app_version', 20)->nullable();
            $table->timestampTz('created_at')->useCurrent();
        });

        DB::statement('CREATE INDEX idx_parser_reports_app_key ON parser_reports (app_key)');
        DB::statement('CREATE INDEX idx_parser_reports_success ON parser_reports (success)');
        DB::statement('CREATE INDEX idx_parser_reports_selector_version ON parser_reports (selector_version)');
        DB::statement('CREATE INDEX idx_parser_reports_created_at ON parser_reports (created_at DESC)');
        DB::statement('CREATE INDEX idx_parser_reports_device_id ON parser_reports (device_id)');
    }

    public function down(): void
    {
        Schema::dropIfExists('parser_reports');
    }
};
