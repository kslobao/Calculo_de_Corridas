<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Jobs\ValidateGoogleSubscription;
use App\Models\Subscription;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class SubscriptionController extends Controller
{
    public function index(Request $request): View
    {
        $query = Subscription::with(['license.device'])
            ->latest('updated_at');

        if ($request->filled('status')) {
            $query->where('status', $request->input('status'));
        }

        $subscriptions = $query->paginate(20);
        return view('admin.subscriptions.index', compact('subscriptions'));
    }

    public function show(Subscription $subscription): View
    {
        $subscription->load(['license.device', 'license.user']);
        return view('admin.subscriptions.show', compact('subscription'));
    }

    public function validate(Request $request, Subscription $subscription): RedirectResponse
    {
        ValidateGoogleSubscription::dispatch($subscription->license_id);
        return back()->with('success', 'Validação com Google Play iniciada em background.');
    }
}
