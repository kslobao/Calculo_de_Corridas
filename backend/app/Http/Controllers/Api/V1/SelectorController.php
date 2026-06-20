<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Services\SelectorService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;

class SelectorController extends Controller
{
    public function __construct(private readonly SelectorService $selectorService) {}

    public function index(Request $request): JsonResponse|Response
    {
        $clientVersion = (int) $request->query('version', 0);

        if ($clientVersion > 0 && $this->selectorService->isVersionCurrent($clientVersion)) {
            return response('', 304);
        }

        $payload = $this->selectorService->getAndroidPayload();

        if (!$payload) {
            return response()->json(['message' => 'No active selector version found.'], 404);
        }

        return response()->json($payload);
    }
}
