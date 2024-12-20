package dti.crosemont.reservationvol.AccesAuxDonnees.SourcesDeDonnees

import dti.crosemont.reservationvol.Domaine.Modele.Vol
import dti.crosemont.reservationvol.Domaine.Modele.VolStatut
import dti.crosemont.reservationvol.Domaine.Modele.`Siège`


import java.time.LocalDateTime

interface VolsDAO: DAO<Vol>{
    override fun chercherTous() : List<Vol>
    override fun chercherParId(id: Int): Vol?
    override fun effacer(id: Int)
    fun obtenirVolParParam(dateDebut: LocalDateTime, aeroportDebut: String, aeroportFin: String): List<Vol>
    fun ajouterVol(vol: Vol): Vol
    fun ajouterStatutVol(volId: Int, statut: VolStatut)
    fun ajouterPrixParClasse(volId: Int, prixParClasse: Map<String, Double>)
    fun trajetExiste(id: Int): Boolean
    fun avionExiste(id: Int): Boolean
    fun volExiste(vol: Vol) : Boolean
    fun modifierVol(id: Int, modifieVol: Vol): Vol
    fun obtenirSiegeParVolId(id: Int): List<Siège>

    fun chercherVolsPourDepart(dateActuelle: LocalDateTime): List<Vol>

    fun chercherVolsPourArrive (dateActuelle: LocalDateTime): List<Vol>
}
