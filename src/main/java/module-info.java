module com.tunelterrorouija.tunelterrorouija {
    requires javafx.controls;
    requires javafx.fxml;


    opens es.medac.grupo3.tunel to javafx.fxml;
    exports es.medac.grupo3.tunel;
}