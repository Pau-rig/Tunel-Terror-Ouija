package es.medac.grupo3.tunel;

import java.util.*;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

public class SalaController {

    private static final String PISTA = "ALMA";
    private static final String LETRAS = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";

    @FXML private GridPane tablero;
    @FXML private Pane capa;
    @FXML private ImageView plancheta;
    @FXML private Label lblMensaje, lblLetra, lblAyuda;
    @FXML private Polygon llamaIzq, llamaDer;
    @FXML private Circle resplandorIzq, resplandorDer;
    @FXML private ImageView imgHumo1, imgHumo2;
    @FXML private StackPane paneJumpscare;
    @FXML private ImageView imgJumpscare;

    private final Map<Character, Button> botones = new HashMap<>();
    private int faseActual = 0; // 0: Letra A, 1: Letra L, 2: Letra M, 3: Letra A final
    private int indiceLetraRespuesta = 0;
    private int erroresConsecutivos = 0; // Contador de errores en el acertijo actual

    private SequentialTransition recorrido;
    private MediaPlayer ambiente;

    private final PauseTransition piedad = new PauseTransition(Duration.seconds(60));
    private final PauseTransition autoReinicio = new PauseTransition(Duration.seconds(15));
    private final PauseTransition temporizadorPista = new PauseTransition(Duration.seconds(7));

    private final List<Acertijo> ACERTIJOS = Arrays.asList(
            new Acertijo("ATAUD", "Fase 1: Los espíritus guardan silencio... Donde descansa Dracula."),
            new Acertijo("LAPIDA", "Fase 2: El aire se enfría... Que hay muchas en un cementerio."),
            new Acertijo("MALDICION", "Fase 3: Las velas parpadean... Hechizo de bruja que trae mala suerte."),
            new Acertijo("ARAÑA", "Fase 4 (Última): El tablero tiembla... Animal con 8 patas.")
    );

    private static class Acertijo {
        String palabraRespuesta;
        String pistaOrientativa;

        public Acertijo(String palabraRespuesta, String pistaOrientativa) {
            this.palabraRespuesta = palabraRespuesta;
            this.pistaOrientativa = pistaOrientativa;
        }
    }

    @FXML
    private void initialize() {
        crearTablero();
        iniciarAnimacionVelas();
        iniciarAnimacionHumo();

        // Cargar la imagen de la plancheta de madera (aguja.png)
        try {
            plancheta.setImage(new Image(recurso("img/aguja.png")));
        } catch (Exception e) {
            System.err.println("No se pudo cargar la imagen aguja.png desde la carpeta img.");
        }

        ambiente = new MediaPlayer(new Media(recurso("sonidos/ambiente.mp3")));
        ambiente.setCycleCount(MediaPlayer.INDEFINITE);
        ambiente.setVolume(0.4);
        ambiente.play();

        piedad.setOnFinished(e -> mostrarAyudaExtra());
        autoReinicio.setOnFinished(e -> reiniciar());

        temporizadorPista.setOnFinished(e -> {
            if (faseActual < ACERTIJOS.size()) {
                lblMensaje.setText(ACERTIJOS.get(faseActual).pistaOrientativa);
            }
        });

        reiniciar();
    }

    private void crearTablero() {
        for (int i = 0; i < LETRAS.length(); i++) {
            char letra = LETRAS.charAt(i);
            Button b = new Button(String.valueOf(letra));
            b.getStyleClass().add("letra-ouija");
            b.setOnAction(e -> pulsarLetraTablero(letra));
            botones.put(letra, b);
            tablero.add(b, i % 9, i / 9);
        }
    }

    private void pulsarLetraTablero(char letra) {
        if (paneJumpscare.isVisible()) return;
        temporizadorPista.stop();

        Acertijo acertijoActual = ACERTIJOS.get(faseActual);
        char letraEsperada = acertijoActual.palabraRespuesta.charAt(indiceLetraRespuesta);

        if (letra == letraEsperada) {
            erroresConsecutivos = 0;
            moverPlanchetaHacia(letra);
            indiceLetraRespuesta++;

            if (indiceLetraRespuesta == acertijoActual.palabraRespuesta.length()) {
                completarFaseActual(acertijoActual);
            }
        } else {
            erroresConsecutivos++;
            efecto("fallo.wav");
            temblar(tablero);

            if (erroresConsecutivos >= 3) {
                // ¡3 errores! Revelamos la respuesta entera por compasión de los espíritus
                erroresConsecutivos = 0;
                lblMensaje.setText("¡Tantos errores han enfocado a los espíritus! Os revelan la respuesta completa...");

                // Mueve la plancheta deletreando la respuesta entera del acertijo
                moverPlanchetaTexto(acertijoActual.palabraRespuesta, () -> {
                    completarFaseActual(acertijoActual);
                });
            } else {
                lblMensaje.setText("¡NO! Letra incorrecta (" + erroresConsecutivos + "/3 fallos). Empezad de nuevo este acertijo...");
                indiceLetraRespuesta = 0;
                temporizadorPista.playFromStart();
            }
        }
    }

    private void completarFaseActual(Acertijo acertijoActual) {
        lblMensaje.setText("¡Correcto! Los espíritus revelan una parte de la palabra...");

        char letraAlma = PISTA.charAt(faseActual);
        Button btnAlma = botones.get(letraAlma);
        if (btnAlma != null && !btnAlma.getStyleClass().contains("encendida")) {
            btnAlma.getStyleClass().add("encendida");
        }

        faseActual++;
        indiceLetraRespuesta = 0;
        erroresConsecutivos = 0;

        if (faseActual < ACERTIJOS.size()) {
            PauseTransition siguiente = new PauseTransition(Duration.seconds(2));
            siguiente.setOnFinished(e -> lblMensaje.setText(ACERTIJOS.get(faseActual).pistaOrientativa));
            siguiente.play();
            piedad.playFromStart();
        } else {
            mostrarImagenFinalAlma();
        }
    }

    private void moverPlanchetaHacia(char letra) {
        Button b = botones.get(letra);
        if (b == null) return;
        Bounds caja = b.localToScene(b.getBoundsInLocal());
        Point2D centro = capa.sceneToLocal(caja.getCenterX(), caja.getCenterY());

        TranslateTransition paso = new TranslateTransition(Duration.millis(400), plancheta);
        paso.setToX(centro.getX() - plancheta.getFitWidth() / 2);
        paso.setToY(centro.getY() - plancheta.getFitHeight() / 2);
        paso.play();
    }

    private void moverPlanchetaTexto(String palabra, Runnable onFinish) {
        if (recorrido != null) recorrido.stop();
        recorrido = new SequentialTransition();
        for (char letra : palabra.toCharArray()) {
            Button b = botones.get(letra);
            if (b == null) continue;
            Bounds caja = b.localToScene(b.getBoundsInLocal());
            Point2D centro = capa.sceneToLocal(caja.getCenterX(), caja.getCenterY());
            TranslateTransition paso = new TranslateTransition(Duration.millis(600), plancheta);
            paso.setToX(centro.getX() - plancheta.getFitWidth() / 2);
            paso.setToY(centro.getY() - plancheta.getFitHeight() / 2);
            recorrido.getChildren().addAll(paso, new PauseTransition(Duration.millis(150)));
        }
        if (onFinish != null) {
            recorrido.setOnFinished(e -> onFinish.run());
        }
        recorrido.play();
    }

    private void apagarLetras() {
        faseActual = 0;
        indiceLetraRespuesta = 0;
        erroresConsecutivos = 0;
        botones.values().forEach(b -> b.getStyleClass().removeAll("encendida"));
    }

    public void reiniciar() {
        autoReinicio.stop();
        temporizadorPista.stop();
        apagarLetras();
        paneJumpscare.setVisible(false);
        lblLetra.setVisible(false);
        lblAyuda.setText("");
        lblMensaje.setText(ACERTIJOS.get(0).pistaOrientativa);

        PauseTransition intro = new PauseTransition(Duration.seconds(1));
        intro.setOnFinished(e -> moverPlanchetaTexto("HOLA", null));
        intro.play();
        piedad.playFromStart();
    }

    private void mostrarAyudaExtra() {
        lblAyuda.setText("La ouija se apiada y os da una pista extra en la mente...");
    }

    private void mostrarImagenFinalAlma() {
        piedad.stop();
        temporizadorPista.stop();
        lblMensaje.setText("¡La palabra ALMA se ha completado!");

        efecto("acierto.wav");

        try {
            imgJumpscare.setImage(new Image(recurso("img/jumpscare.jpg")));
        } catch (Exception e) {
            System.err.println("No se pudo cargar la imagen final desde la carpeta img.");
        }

        paneJumpscare.setVisible(true);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), paneJumpscare);
        st.setFromX(0.5); st.setFromY(0.5);
        st.setToX(1.0); st.setToY(1.0);
        st.play();

        autoReinicio.playFromStart();
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
        return getClass().getResource(ruta).toExternalForm();
    }

    public void detener() {
        if (ambiente != null) ambiente.stop();
    }

    private void iniciarAnimacionVelas() {
        Random rnd = new Random();
        Timeline parpadeo = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            double escalaY1 = 0.85 + rnd.nextDouble() * 0.3;
            double escalaX1 = 0.9 + rnd.nextDouble() * 0.2;
            llamaIzq.setScaleY(escalaY1);
            llamaIzq.setScaleX(escalaX1);
            resplandorIzq.setOpacity(0.6 + rnd.nextDouble() * 0.4);

            double escalaY2 = 0.85 + rnd.nextDouble() * 0.3;
            double escalaX2 = 0.9 + rnd.nextDouble() * 0.2;
            llamaDer.setScaleY(escalaY2);
            llamaDer.setScaleX(escalaX2);
            resplandorDer.setOpacity(0.6 + rnd.nextDouble() * 0.4);
        }));
        parpadeo.setCycleCount(Animation.INDEFINITE);
        parpadeo.play();
    }

    private void iniciarAnimacionHumo() {
        try {
            Image textura = new Image(recurso("img/humitoPiengi.png"));
            imgHumo1.setImage(textura);
            imgHumo2.setImage(textura);

            TranslateTransition trans1 = new TranslateTransition(Duration.seconds(35), imgHumo1);
            trans1.setFromX(0); trans1.setToX(-1920);
            trans1.setInterpolator(Interpolator.LINEAR);
            trans1.setCycleCount(Animation.INDEFINITE);

            TranslateTransition trans2 = new TranslateTransition(Duration.seconds(35), imgHumo2);
            trans2.setFromX(0); trans2.setToX(-1920);
            trans2.setInterpolator(Interpolator.LINEAR);
            trans2.setCycleCount(Animation.INDEFINITE);

            trans1.play();
            trans2.play();
        } catch (Exception e) {
            System.err.println("No se ha encontrado humitoPiengi.png en img/");
        }
    }
}