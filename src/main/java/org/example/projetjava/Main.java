package org.example.projetjava;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Image icon = new Image("C:\\TPs Java\\ProjetJava\\src\\main\\resources\\org\\example\\projetjava\\icon.jpg");
        stage.getIcons().add(icon);
        stage.setResizable(false);
        stage.setTitle("Jeu de tir");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}