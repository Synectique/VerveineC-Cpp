package eu.synectique.verveine.extractor.utils;

public class Tracer {
	public String indent;

	public Tracer() {
		this.indent = "#";
	}

	public void up() {
		indent += "  ";
	}
	public void up(String msg) {
		msg("Entering "+msg);
		up();
	}
	public void msg(String msg) {
		System.err.println(indent+msg);
	}
	public void down() {
		// protect against too may tracedown()
		if (indent.length() > 2) {
			indent = indent.substring(0, indent.length()-2);
		}
	}
	public void down(String msg) {
		down();
		msg("Leaving "+msg);
	}

}