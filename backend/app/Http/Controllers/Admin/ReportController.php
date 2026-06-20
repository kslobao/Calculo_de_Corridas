<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\ParserReport;
use Illuminate\Http\Request;
use Illuminate\View\View;

class ReportController extends Controller
{
    public function index(Request $request): View
    {
        $query = ParserReport::with('device')->latest();

        if ($request->filled('app_key')) {
            $query->where('app_key', $request->input('app_key'));
        }

        if ($request->filled('success')) {
            $query->where('success', $request->boolean('success'));
        }

        if ($request->filled('selector_version')) {
            $query->where('selector_version', $request->input('selector_version'));
        }

        $reports = $query->paginate(30);

        $failureStats = ParserReport::selectRaw("app_key, COUNT(*) as failures")
            ->where('success', false)
            ->where('created_at', '>=', now()->subDays(7))
            ->groupBy('app_key')
            ->get();

        return view('admin.reports.index', compact('reports', 'failureStats'));
    }

    public function show(ParserReport $report): View
    {
        $report->load('device');
        return view('admin.reports.show', compact('report'));
    }
}
