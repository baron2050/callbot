package org.asteriskjava.fastagi.command;

public class GetAudioStreamCommand extends AbstractAgiCommand
{
	/**
	 * Serial version identifier
	 */
	private static final long serialVersionUID = -315771117377393409L;

	/**
	 * the audio stream server host.
	 */
	private String host;

	/**
	 * the audio stream server port.
	 */
	private int port;

	/**
	 * Create a new GetAudioStreamCommand
	 * @param host the audio stream server host to accept tcp connect
	 * @param port the audio stream server port to accept tcp connect
	 */
	public GetAudioStreamCommand(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}

	@Override
	public String buildCommand() {
		return "GETAUDIOSTREAM " + host + " " + port;
	}

	/**
	 * Returns the audio stream server host.
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the audio stream server port
	 * @return
	 */
	public int getPort() {
		return port;
	}
}