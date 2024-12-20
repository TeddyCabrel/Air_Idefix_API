package dti.crosemont.reservationvol.AccesAuxDonnees.SourcesDeDonnees

import dti.crosemont.reservationvol.Domaine.Modele.Reservation

interface ReservationsDAO: DAO<Reservation>{
        fun ajouterRéservation(réservation: Reservation): Reservation
        fun modifierRéservation(id: Int, réservation: Reservation): Reservation
        override fun effacer(id: Int)
        fun chercherTous(idClient: Int): List<Reservation>
        fun modifierSiègeVol( réservation: Reservation )
}
