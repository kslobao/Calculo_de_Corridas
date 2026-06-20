<?php

namespace App\Http\Requests\Admin;

use App\Enums\AppKey;
use App\Enums\FieldType;
use App\Enums\SelectorType;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rules\Enum;

class SelectorStoreRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'version_id'    => ['required', 'integer', 'exists:selector_versions,id'],
            'app_key'       => ['required', new Enum(AppKey::class)],
            'field_type'    => ['required', new Enum(FieldType::class)],
            'selector_type' => ['required', new Enum(SelectorType::class)],
            'pattern_value' => ['required', 'string', 'max:2000'],
            'priority'      => ['nullable', 'integer', 'min:1', 'max:1000'],
            'is_active'     => ['sometimes', 'boolean'],
            'notes'         => ['nullable', 'string', 'max:1000'],
        ];
    }
}
