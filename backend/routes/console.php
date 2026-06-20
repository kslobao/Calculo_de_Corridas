<?php

use App\Console\Commands\CleanOldParserReports;
use App\Console\Commands\SyncExpiredSubscriptions;
use Illuminate\Support\Facades\Schedule;

Schedule::command(SyncExpiredSubscriptions::class)->hourly()->withoutOverlapping();
Schedule::command(CleanOldParserReports::class)->dailyAt('03:00')->withoutOverlapping();
