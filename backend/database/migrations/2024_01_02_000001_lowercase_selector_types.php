<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    public function up(): void
    {
        DB::statement("UPDATE selectors SET selector_type = LOWER(selector_type)");
        DB::statement("ALTER TABLE selectors DROP CONSTRAINT IF EXISTS chk_selectors_selector_type");
        DB::statement("ALTER TABLE selectors ADD CONSTRAINT chk_selectors_selector_type CHECK (selector_type IN ('accessibility_id','regex','content_desc','class_name'))");
    }

    public function down(): void
    {
        DB::statement("UPDATE selectors SET selector_type = UPPER(selector_type)");
        DB::statement("ALTER TABLE selectors DROP CONSTRAINT IF EXISTS chk_selectors_selector_type");
        DB::statement("ALTER TABLE selectors ADD CONSTRAINT chk_selectors_selector_type CHECK (selector_type IN ('ACCESSIBILITY_ID','REGEX','CONTENT_DESC','CLASS_NAME'))");
    }
};
