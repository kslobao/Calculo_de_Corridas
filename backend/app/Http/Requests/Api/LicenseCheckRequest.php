<?php

namespace App\Http\Requests\Api;

use Illuminate\Foundation\Http\FormRequest;

class LicenseCheckRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'device_id'      => ['required', 'string', 'size:64'],
            'package_name'   => ['required', 'string', 'max:100'],
            'purchase_token' => ['nullable', 'string', 'max:500'],
        ];
    }
}
