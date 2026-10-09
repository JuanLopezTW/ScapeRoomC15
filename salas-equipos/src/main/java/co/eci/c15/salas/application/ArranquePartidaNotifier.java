package co.eci.c15.salas.application;

/** Avisa a los jugadores de la sala como va el arranque de la partida (HU-64.4). */
public interface ArranquePartidaNotifier {
    void cuentaRegresiva(String salaId, long segundos);
    void cancelada(String salaId);
    void iniciada(String salaId, String matchId);
}
