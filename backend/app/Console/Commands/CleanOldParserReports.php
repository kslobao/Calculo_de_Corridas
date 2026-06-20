<?php

namespace App\Console\Commands;

use App\Models\ParserReport;
use Illuminate\Console\Command;

class CleanOldParserReports extends Command
{
    protected $signature   = 'reports:clean {--days=90 : Reports older than this many days will be deleted}';
    protected $description = 'Delete old parser reports to keep the database lean.';

    public function handle(): int
    {
        $days    = (int) $this->option('days');
        $cutoff  = now()->subDays($days);
        $deleted = ParserReport::where('created_at', '<', $cutoff)->delete();

        $this->info("Deleted {$deleted} parser reports older than {$days} days.");

        return self::SUCCESS;
    }
}
