<?php

namespace App\Services;

use App\Models\AuditLog;
use Illuminate\Http\Request;

class AuditService
{
    public function log(
        int $adminUserId,
        string $action,
        ?string $modelType = null,
        ?string $modelId = null,
        ?array $oldValues = null,
        ?array $newValues = null,
        ?Request $request = null,
    ): AuditLog {
        return AuditLog::create([
            'admin_user_id' => $adminUserId,
            'action'        => $action,
            'model_type'    => $modelType,
            'model_id'      => $modelId,
            'old_values'    => $oldValues,
            'new_values'    => $newValues,
            'ip_address'    => $request?->ip(),
            'user_agent'    => $request?->userAgent(),
        ]);
    }
}
