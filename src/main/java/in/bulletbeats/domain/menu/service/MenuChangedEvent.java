package in.bulletbeats.domain.menu.service;

/**
 * Published whenever a menu item or category changes. {@link MenuCache} reloads its snapshot
 * once the publishing transaction commits (never on rollback).
 */
public record MenuChangedEvent() {}
