<?php

namespace App\Console\Commands;

use App\Jobs\SyncExpiredSubscriptionsJob;
use Illuminate\Console\Command;

class SyncExpiredSubscriptions extends Command
{
    protected $signature   = 'subscriptions:sync-expired';
    protected $description = 'Mark expired Google Play subscriptions and downgrade associated licenses to free.';

    public function handle(): int
    {
        $this->info('Dispatching SyncExpiredSubscriptionsJob...');
        SyncExpiredSubscriptionsJob::dispatch();
        $this->info('Job dispatched.');

        return self::SUCCESS;
    }
}
