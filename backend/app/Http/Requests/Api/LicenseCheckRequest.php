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
            'deviceId'      => ['required', 'string', 'size:64'],
            'packageName'   => ['required', 'string', 'max:100'],
            'purchaseToken' => ['nullable', 'string', 'max:500'],
        ];
    }
}
