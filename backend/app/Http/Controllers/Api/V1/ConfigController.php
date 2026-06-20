<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Services\ConfigService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ConfigController extends Controller
{
    public function __construct(private readonly ConfigService $configService) {}

    public function index(Request $request): JsonResponse
    {
        return response()->json($this->configService->getPublicConfig());
    }
}
