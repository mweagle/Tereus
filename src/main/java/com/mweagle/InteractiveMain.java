package com.mweagle;

import static spark.Spark.*;

public class InteractiveMain {

	 public static void main(String[] args) {
		 	final String clazzPath = System.getProperty("java.class.path");
	        staticFileLocation("/web");
	        get("/hello", (request, response) -> "Hello World!");
	    }
}
