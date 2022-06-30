package fr.verveine.utils;

public class Trace {

	public static void trace(String msg) {
		System.out.println(msg);
	}

	public static void trace(boolean condition, String msg) {
		if (condition) {
			trace(msg);
		}
	}
}
