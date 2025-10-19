module j_project {
		requires java.base;
	    requires transitive java.logging;
		requires javafx.controls;
		requires transitive javafx.graphics;
	    requires javafx.fxml;
	    requires org.junit.jupiter.api;

	    exports client;
	    exports server;
	    exports common;
	    exports utils;
	    exports main;
	    exports JUnitTests;
}