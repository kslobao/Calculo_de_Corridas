<?php

namespace App\Http\Requests\Api;

use App\Enums\AppKey;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rules\Enum;

class ParserReportRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'app_key'          => ['required', 'string', new Enum(AppKey::class)],
            'selector_version' => ['nullable', 'integer', 'min:1'],
            'raw_texts'        => ['nullable', 'array'],
            'raw_texts.*'      => ['string', 'max:500'],
            'parsed_value'     => ['nullable', 'numeric', 'min:0'],
            'parsed_distance'  => ['nullable', 'numeric', 'min:0'],
            'parsed_duration'  => ['nullable', 'integer', 'min:0'],
            'success'          => ['nullable', 'boolean'],
            'error_message'    => ['nullable', 'string', 'max:1000'],
            'app_version'      => ['nullable', 'string', 'max:20'],
            // Legacy Android fields — accepted but mapped internally
            'failed_pattern'   => ['nullable', 'string', 'max:500'],
            'event_type'       => ['nullable', 'string', 'max:100'],
            'timestamp'        => ['nullable', 'integer'],
        ];
    }
}
