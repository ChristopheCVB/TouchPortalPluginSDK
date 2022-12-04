package com.christophecvb.touchportal;

public abstract class TPAction<T extends TouchPortalPlugin> extends TPInvokable<T> {

  public TPAction(T touchPortalPlugin) {
    super(touchPortalPlugin);
  }

  protected Boolean isBeingHeld(String actionId) {
    return this.touchPortalPlugin.isActionBeingHeld(actionId);
  }
}