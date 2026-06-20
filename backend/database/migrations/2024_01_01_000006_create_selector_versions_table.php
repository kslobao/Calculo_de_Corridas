<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('selector_versions', function (Blueprint $table) {
            $table->increments('id');
            $table->unsignedInteger('version')->unique();
            $table->text('description')->nullable();
            $table->boolean('is_active')->default(false);
            $table->unsignedInteger('published_by')->nullable();
            $table->foreign('published_by')->references('id')->on('admin_users')->nullOnDelete();
            $table->timestampTz('published_at')->nullable();
            $table->timestampTz('created_at')->useCurrent();
        });

        DB::statement('CREATE UNIQUE INDEX idx_selector_versions_active ON selector_versions (is_active) WHERE is_active = true');
    }

    public function down(): void
    {
        Schema::dropIfExists('selector_versions');
    }
};
