<?php

namespace App\Enums;

enum SelectorType: string
{
    case AccessibilityId = 'accessibility_id';
    case Regex           = 'regex';
    case ContentDesc     = 'content_desc';
    case ClassName       = 'class_name';

    public function label(): string
    {
        return match($this) {
            self::AccessibilityId => 'Accessibility ID (ViewId)',
            self::Regex           => 'Expressão Regular',
            self::ContentDesc     => 'Content Description',
            self::ClassName       => 'Class Name',
        };
    }

    public function description(): string
    {
        return match($this) {
            self::AccessibilityId => 'Ex: com.ubercab.driver:id/trip_fare',
            self::Regex           => 'Ex: R\\$\\s*([\\d.,]+)',
            self::ContentDesc     => 'Texto visível no contentDescription do nó',
            self::ClassName       => 'Ex: android.widget.TextView',
        };
    }
}
