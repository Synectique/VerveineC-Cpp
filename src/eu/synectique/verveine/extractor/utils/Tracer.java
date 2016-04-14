package eu.synectique.verveine.extractor.utils;

public class Tracer implements ITracer {
	private String indent;

	public Tracer() {
		this.indent = "#";
	}

	@Override
	public void up() {
		indent += "  ";
	}

	@Override
	public void up(String msg) {
		msg("Entering "+msg);
		up();
	}

	@Override
	public void msg(String msg) {
			System.err.println(indent+msg);
			System.err.flush();
	}

	@Override
	public void down() {
		// protect against too may tracedown()
		if (indent.length() > 2) {
			indent = indent.substring(0, indent.length()-2);
		}
	}

	@Override
	public void down(String msg) {
		down();
		msg("Leaving "+msg);
	}

}