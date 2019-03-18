package org.asteriskjava.fastagi.command;

public class StopStreamCommand extends AbstractAgiCommand {

	private static final long serialVersionUID = 851466238042602794L;

	@Override
	public String buildCommand() {
		return "STOPSTREAM";
	}
}
