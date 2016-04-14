package eu.synectique.verveine.extractor.utils;

public class NullTracer implements ITracer {

	public NullTracer() {}

	@Override
	public void up() {}

	@Override
	public void up(String msg) {}

	@Override
	public void msg(String msg) {}

	@Override
	public void down() {}

	@Override
	public void down(String msg) {}

}
