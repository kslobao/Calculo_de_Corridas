<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\Api\ParserReportRequest;
use App\Models\Device;
use App\Models\ParserReport;
use Illuminate\Http\JsonResponse;

class ParserReportController extends Controller
{
    public function store(ParserReportRequest $request): JsonResponse
    {
        /** @var Device $device */
        $device = $request->attributes->get('device');

        ParserReport::create([
            'device_id'           => $device->id,
            'app_key'             => $request->input('app_key'),
            'selector_version'    => $request->input('selector_version'),
            'raw_texts'           => $request->input('raw_texts'),
            'parsed_value'        => $request->input('parsed_value'),
            'parsed_distance'     => $request->input('parsed_distance'),
            'parsed_duration_min' => $request->input('parsed_duration'),
            'success'             => $request->boolean('success'),
            'error_message'       => $request->input('error_message'),
            'app_version'         => $request->input('app_version'),
        ]);

        return response()->json(['received' => true], 201);
    }
}
