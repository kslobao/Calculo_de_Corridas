<?php

namespace App\Http\Requests\Admin;

use Illuminate\Foundation\Http\FormRequest;

class LicenseUpdateRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'plan'       => ['sometimes', 'in:free,pro'],
            'is_active'  => ['sometimes', 'boolean'],
            'expires_at' => ['nullable', 'date'],
            'reason'     => ['nullable', 'string', 'max:500'],
            'notes'      => ['nullable', 'string', 'max:1000'],
        ];
    }
}
