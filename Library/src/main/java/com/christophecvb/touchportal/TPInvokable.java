package com.christophecvb.touchportal;

import com.christophecvb.touchportal.model.TPListChangedMessage;

abstract class TPInvokable<T extends TouchPortalPlugin> {
  protected final T touchPortalPlugin;

  public TPInvokable(T touchPortalPlugin) {
    this.touchPortalPlugin = touchPortalPlugin;
  }

  public abstract void onInvoke();

  public abstract void onListChanged(TPListChangedMessage tpListChangedMessage);
}