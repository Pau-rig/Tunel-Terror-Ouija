package es.medac.grupo3.tunel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Apunta correctamente a Sala.fxml
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Sala.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("El Túnel - Sala Ouija");
        stage.setScene(scene);

        // Pantalla completa
        stage.setFullScreen(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
