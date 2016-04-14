package eu.synectique.verveine.extractor.utils;

public interface ITracer {

	public void up();
	public void up(String msg);
	public void msg(String msg);
	public void down();
	public void down(String msg);

}
