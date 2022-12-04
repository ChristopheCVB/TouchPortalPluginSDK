package com.christophecvb.touchportal;

public abstract class TPConnector<T extends TouchPortalPlugin> extends TPInvokable<T> {

  public TPConnector(T touchPortalPlugin) {
    super(touchPortalPlugin);
  }
}