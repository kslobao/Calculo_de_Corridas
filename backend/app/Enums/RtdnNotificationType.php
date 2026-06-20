<?php

namespace App\Enums;

enum RtdnNotificationType: int
{
    case Recovered              = 1;
    case Renewed                = 2;
    case Canceled               = 3;
    case Purchased              = 4;
    case OnHold                 = 5;
    case InGracePeriod          = 6;
    case Restarted              = 7;
    case PriceChangeConfirmed   = 8;
    case Deferred               = 9;
    case Paused                 = 10;
    case PauseScheduleChanged   = 11;
    case Revoked                = 12;
    case Expired                = 13;

    public function targetSubscriptionStatus(): SubscriptionStatus
    {
        return match($this) {
            self::Purchased, self::Recovered, self::Renewed, self::Restarted => SubscriptionStatus::Active,
            self::Canceled              => SubscriptionStatus::Cancelled,
            self::OnHold                => SubscriptionStatus::OnHold,
            self::InGracePeriod         => SubscriptionStatus::GracePeriod,
            self::Paused, self::PauseScheduleChanged => SubscriptionStatus::Paused,
            self::Revoked               => SubscriptionStatus::Revoked,
            self::Expired               => SubscriptionStatus::Expired,
            default                     => SubscriptionStatus::Active,
        };
    }

    public function shouldDeactivateLicense(): bool
    {
        return match($this) {
            self::Revoked, self::Expired => true,
            default                      => false,
        };
    }
}
