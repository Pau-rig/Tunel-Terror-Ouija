package es.medac.grupo3.tunel;

import java.util.*;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

public class SalaController {

    private static final String LETRA = "M";
    private static final String PISTA = "ALMA";   // palabra acordada con 1.º (solo letras del tablero)
    private static final String LETRAS = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";

    @FXML private GridPane tablero;
    @FXML private Pane capa;
    @FXML private Label plancheta, lblMensaje, lblLetra, lblAyuda;

    private final Map<Character, Button> botones = new HashMap<>();
    private int posicion;                  // cuantas letras de la pista llevan acertadas
    private SequentialTransition recorrido;  // movimiento actual de la plancheta
    private MediaPlayer ambiente;
    private final PauseTransition piedad = new PauseTransition(Duration.seconds(60));
    private final PauseTransition autoReinicio = new PauseTransition(Duration.seconds(12));

    @FXML
    private void initialize() {
        crearTablero();
        ambiente = new MediaPlayer(new Media(recurso("sonidos/ambiente.mp3")));
        ambiente.setCycleCount(MediaPlayer.INDEFINITE);
        ambiente.setVolume(0.4);
        ambiente.play();
        piedad.setOnFinished(e -> mostrarAyuda());
        autoReinicio.setOnFinished(e -> reiniciar());
        reiniciar();
    }

    // Crea los 27 botones del tablero en 3 filas de 9
    private void crearTablero() {
        for (int i = 0; i < LETRAS.length(); i++) {
            char letra = LETRAS.charAt(i);
            Button b = new Button(String.valueOf(letra));
            b.getStyleClass().add("letra-ouija");
            b.setOnAction(e -> pulsar(letra));
            botones.put(letra, b);
            tablero.add(b, i % 9, i / 9);
        }
    }

    private void pulsar(char letra) {
        if (lblLetra.isVisible()) return;
        if (letra == PISTA.charAt(posicion)) {
            if (!botones.get(letra).getStyleClass().contains("encendida")) botones.get(letra).getStyleClass().add("encendida");
            moverPlancheta(String.valueOf(letra));
            posicion++;
            if (posicion == PISTA.length()) {
                lblMensaje.setText("Los espíritus os responden...");
                mostrarLetra();
            }
        } else {
            efecto("fallo.wav");
            lblMensaje.setText("¡NO! Empezad de nuevo...");
            apagarLetras();
            temblar(tablero);
        }
    }

    // Mueve la plancheta letra a letra por la palabra indicada
    private void moverPlancheta(String palabra) {
        if (recorrido != null) recorrido.stop();
        recorrido = new SequentialTransition();
        for (char letra : palabra.toCharArray()) {
            Button b = botones.get(letra);
            if (b == null) continue;
            Bounds caja = b.localToScene(b.getBoundsInLocal());
            Point2D centro = capa.sceneToLocal(caja.getCenterX(), caja.getCenterY());
            TranslateTransition paso = new TranslateTransition(Duration.millis(900), plancheta);
            paso.setToX(centro.getX() - plancheta.getWidth() / 2);
            paso.setToY(centro.getY() - plancheta.getHeight() / 2);
            recorrido.getChildren().addAll(paso, new PauseTransition(Duration.millis(300)));
        }
        recorrido.play();
    }

    private void apagarLetras() {
        posicion = 0;
        botones.values().forEach(b -> b.getStyleClass().removeAll("encendida"));
    }

    public void reiniciar() {
        autoReinicio.stop();
        apagarLetras();
        lblLetra.setVisible(false);
        lblAyuda.setText("");
        lblMensaje.setText("Pulsad las letras que os han susurrado...");
        // Esperamos 1 s a que la ventana se dibuje y la plancheta saluda
        PauseTransition intro = new PauseTransition(Duration.seconds(1));
        intro.setOnFinished(e -> moverPlancheta("HOLA"));
        intro.play();
        piedad.playFromStart();
    }

    private void mostrarAyuda() {
        lblAyuda.setText("La ouija se apiada de vosotros y os señala el camino...");
        moverPlancheta(PISTA.substring(posicion));
    }

    // ---------- Comun a todas las salas ----------
    private void mostrarLetra() {
        piedad.stop();
        lblLetra.setText(LETRA);
        lblLetra.setVisible(true);
        ScaleTransition escala = new ScaleTransition(Duration.millis(700), lblLetra);
        escala.setFromX(0.1); escala.setFromY(0.1);
        escala.setToX(1); escala.setToY(1);
        FadeTransition aparicion = new FadeTransition(Duration.millis(700), lblLetra);
        aparicion.setFromValue(0); aparicion.setToValue(1);
        new ParallelTransition(escala, aparicion).play();
        efecto("acierto.wav");
        autoReinicio.playFromStart();   // la sala se reinicia sola para el siguiente grupo
    }

    private void temblar(Node nodo) {
        TranslateTransition t = new TranslateTransition(Duration.millis(50), nodo);
        t.setFromX(-8); t.setToX(8);
        t.setCycleCount(6); t.setAutoReverse(true);
        t.setOnFinished(e -> nodo.setTranslateX(0));
        t.play();
    }

    private void efecto(String archivo) {
        new AudioClip(recurso("sonidos/" + archivo)).play();
    }

    private String recurso(String ruta) {
        // Si sale NullPointerException aqui: el archivo no esta en resources o el nombre no coincide
        return getClass().getResource(ruta).toExternalForm();
    }

    public void detener() {
        ambiente.stop();
    }
}