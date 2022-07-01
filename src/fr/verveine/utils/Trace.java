package fr.verveine.utils;

public class Trace {

	protected static boolean active = true;

	public static void trace(String msg) {
		if (active) {
			System.out.println(msg);
		}
	}

	public static void trace(boolean condition, String msg) {
		if (condition) {
			trace(msg);
		}
	}

	public static void off() {
		active = false;
	}

	public static void on() {
		active = true;
	}
}
