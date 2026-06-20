<?php

namespace App\Http\Requests\Admin;

use App\Enums\LicenseSource;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rules\Enum;

class LicenseStoreRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'device_id'  => ['nullable', 'uuid', 'exists:devices,id'],
            'user_id'    => ['nullable', 'uuid', 'exists:users,id'],
            'source'     => ['required', new Enum(LicenseSource::class)],
            'expires_at' => ['nullable', 'date', 'after:now'],
            'reason'     => ['nullable', 'string', 'max:500'],
            'notes'      => ['nullable', 'string', 'max:1000'],
        ];
    }
}
