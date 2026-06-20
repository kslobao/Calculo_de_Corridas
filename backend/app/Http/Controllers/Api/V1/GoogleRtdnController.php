<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Services\RtdnService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Log;

class GoogleRtdnController extends Controller
{
    public function __construct(private readonly RtdnService $rtdnService) {}

    public function handle(Request $request): Response
    {
        $expectedToken = config('services.google_play.rtdn_token');
        $providedToken = $request->query('token');

        if ($expectedToken && $providedToken !== $expectedToken) {
            Log::warning('RTDN: invalid token', ['ip' => $request->ip()]);
            return response('', 401);
        }

        $payload = $request->json()->all();
        $this->rtdnService->handle($payload);

        // Always return 200 to Google so Pub/Sub doesn't retry unnecessarily
        return response('', 200);
    }
}
