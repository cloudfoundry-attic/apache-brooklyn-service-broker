package org.cloudfoundry.community.servicebroker.brooklyn.service;

public class PollingException extends Exception {
	public PollingException(Exception e) {
		super(e);
	}
}
