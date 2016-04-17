package eu.synectique.verveine.extractor.utils;

public class Tracer implements ITracer {
	private String indent;
	private String prefix;

	public Tracer() {
		this("#");
	}

	public Tracer(String prefix) {
		this.indent = "";
		this.prefix = prefix;
	}

	@Override
	public void up(String msg) {
		msg(msg);
		up();
	}

	@Override
	public void up() {
		indent += "  ";
	}

	@Override
	public void msg(String msg) {
			System.err.println(prefix+indent+msg);
			System.err.flush();
	}

	@Override
	public void down(String msg) {
		down();
		msg(msg);
	}

	@Override
	public void down() {
		// protect against too may tracedown()
		if (indent.length() > 1) {
			indent = indent.substring(2);
		}
	}

}