package org.asteriskjava.fastagi.command;

public class AsyncStreamFileCommand extends AbstractAgiCommand {

	private static final long serialVersionUID = 8700646155408479878L;

	private String file;

	public AsyncStreamFileCommand(String file) {
		super();
		this.file = file;
	}

	@Override
	public String buildCommand() {
		return "ASYNCSTREAM FILE " + file;
	}
}
