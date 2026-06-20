<?php

namespace App\Services;

use Firebase\JWT\JWT;
use GuzzleHttp\Client;
use GuzzleHttp\Exception\ClientException;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class GooglePlayService
{
    private const TOKEN_ENDPOINT = 'https://oauth2.googleapis.com/token';
    private const API_BASE       = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications';
    private const SCOPE          = 'https://www.googleapis.com/auth/androidpublisher';
    private const TOKEN_CACHE    = 'google_play:access_token';

    private array $serviceAccount;

    public function __construct(private readonly Client $http)
    {
        $this->serviceAccount = json_decode(
            config('services.google_play.service_account_json', '{}'),
            true,
        );
    }

    public function getSubscription(string $subscriptionId, string $purchaseToken): ?array
    {
        $packageName = config('services.google_play.package_name');

        try {
            $accessToken = $this->getAccessToken();

            $response = $this->http->get(
                self::API_BASE."/{$packageName}/purchases/subscriptions/{$subscriptionId}/tokens/{$purchaseToken}",
                ['headers' => ['Authorization' => "Bearer {$accessToken}"]]
            );

            return json_decode($response->getBody()->getContents(), true);
        } catch (ClientException $e) {
            Log::warning('GooglePlayService: subscription lookup failed', [
                'subscription_id'  => $subscriptionId,
                'status'           => $e->getResponse()->getStatusCode(),
                'body'             => $e->getResponse()->getBody()->getContents(),
            ]);
            return null;
        } catch (\Throwable $e) {
            Log::error('GooglePlayService: unexpected error', ['error' => $e->getMessage()]);
            return null;
        }
    }

    public function isSubscriptionActive(array $googleResponse): bool
    {
        $paymentState  = $googleResponse['paymentState'] ?? null;
        $expiryMillis  = (int) ($googleResponse['expiryTimeMillis'] ?? 0);
        $cancelReason  = $googleResponse['cancelReason'] ?? null;

        if ($paymentState === null) {
            return false;
        }

        $isExpired   = $expiryMillis > 0 && $expiryMillis < (time() * 1000);
        $isCancelled = $cancelReason !== null && $isExpired;

        return !$isCancelled && !$isExpired && in_array($paymentState, [0, 1, 2], true);
    }

    public function extractExpiryDate(array $googleResponse): ?\Carbon\Carbon
    {
        $millis = (int) ($googleResponse['expiryTimeMillis'] ?? 0);
        return $millis > 0 ? \Carbon\Carbon::createFromTimestampMs($millis) : null;
    }

    public function extractOrderId(array $googleResponse): ?string
    {
        return $googleResponse['orderId'] ?? null;
    }

    public function extractStartTime(array $googleResponse): ?\Carbon\Carbon
    {
        $millis = (int) ($googleResponse['startTimeMillis'] ?? 0);
        return $millis > 0 ? \Carbon\Carbon::createFromTimestampMs($millis) : null;
    }

    private function getAccessToken(): string
    {
        return Cache::remember(self::TOKEN_CACHE, 3500, function () {
            $now = time();

            $jwt = JWT::encode([
                'iss'   => $this->serviceAccount['client_email'],
                'sub'   => $this->serviceAccount['client_email'],
                'scope' => self::SCOPE,
                'aud'   => self::TOKEN_ENDPOINT,
                'iat'   => $now,
                'exp'   => $now + 3600,
            ], $this->serviceAccount['private_key'], 'RS256', $this->serviceAccount['private_key_id']);

            $response = $this->http->post(self::TOKEN_ENDPOINT, [
                'form_params' => [
                    'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                    'assertion'  => $jwt,
                ],
            ]);

            $data = json_decode($response->getBody()->getContents(), true);

            return $data['access_token'];
        });
    }
}
