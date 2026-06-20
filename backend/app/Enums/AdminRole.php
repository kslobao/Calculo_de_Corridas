<?php

namespace App\Enums;

enum AdminRole: string
{
    case Admin  = 'admin';
    case Editor = 'editor';
    case Viewer = 'viewer';

    public function label(): string
    {
        return match($this) {
            self::Admin  => 'Administrador',
            self::Editor => 'Editor',
            self::Viewer => 'Visualizador',
        };
    }

    public function canEdit(): bool
    {
        return $this === self::Admin || $this === self::Editor;
    }

    public function canDelete(): bool
    {
        return $this === self::Admin;
    }

    public function canPublishSelectors(): bool
    {
        return $this === self::Admin || $this === self::Editor;
    }
}
