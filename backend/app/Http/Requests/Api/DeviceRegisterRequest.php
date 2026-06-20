<?php

namespace App\Http\Requests\Api;

use Illuminate\Foundation\Http\FormRequest;

class DeviceRegisterRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'device_token' => ['required', 'string', 'size:64', 'regex:/^[a-f0-9]+$/'],
            'package_name' => ['required', 'string', 'max:100'],
            'app_version'  => ['nullable', 'string', 'max:20'],
            'platform'     => ['nullable', 'string', 'in:android,ios'],
        ];
    }
}
