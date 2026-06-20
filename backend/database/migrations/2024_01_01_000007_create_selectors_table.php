<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('selectors', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->unsignedInteger('version_id');
            $table->foreign('version_id')->references('id')->on('selector_versions')->cascadeOnDelete();
            $table->string('app_key', 20);
            $table->string('field_type', 30);
            $table->string('selector_type', 30);
            $table->text('pattern_value');
            $table->smallInteger('priority')->default(10);
            $table->boolean('is_active')->default(true);
            $table->text('notes')->nullable();
            $table->timestampsTz();
        });

        DB::statement("ALTER TABLE selectors ADD CONSTRAINT chk_selectors_app_key CHECK (app_key IN ('uber','99','indrive','ifood'))");
        DB::statement("ALTER TABLE selectors ADD CONSTRAINT chk_selectors_field_type CHECK (field_type IN ('price','distance','time','origin','destination','category'))");
        DB::statement("ALTER TABLE selectors ADD CONSTRAINT chk_selectors_selector_type CHECK (selector_type IN ('ACCESSIBILITY_ID','REGEX','CONTENT_DESC','CLASS_NAME'))");
        DB::statement('CREATE INDEX idx_selectors_version_app ON selectors (version_id, app_key)');
        DB::statement('CREATE INDEX idx_selectors_app_field ON selectors (app_key, field_type, is_active)');
        DB::statement('CREATE INDEX idx_selectors_is_active ON selectors (is_active)');
    }

    public function down(): void
    {
        Schema::dropIfExists('selectors');
    }
};
