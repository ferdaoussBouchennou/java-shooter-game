module org.example.projetjava {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;

    opens org.example.projetjava to javafx.fxml;
    exports org.example.projetjava;
    exports org.example.projetjava.controller;
    opens org.example.projetjava.controller to javafx.fxml;
    exports org.example.projetjava.model;
    opens org.example.projetjava.model to javafx.fxml;
    exports org.example.projetjava.manager;
    opens org.example.projetjava.manager to javafx.fxml;
}