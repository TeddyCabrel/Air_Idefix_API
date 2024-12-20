package dti.crosemont.reservationvol.AccesAuxDonnees.BD

import dti.crosemont.reservationvol.AccesAuxDonnees.SourcesDeDonnees.VolsDAO
import dti.crosemont.reservationvol.Controleurs.Exceptions.RessourceInexistanteException
import dti.crosemont.reservationvol.Domaine.Modele.Aeroport
import dti.crosemont.reservationvol.Domaine.Modele.Avion
import dti.crosemont.reservationvol.Domaine.Modele.Trajet
import dti.crosemont.reservationvol.Domaine.Modele.Ville
import dti.crosemont.reservationvol.Domaine.Modele.Vol
import dti.crosemont.reservationvol.Domaine.Modele.VolStatut
import dti.crosemont.reservationvol.Domaine.Modele.`Siège`
import dti.crosemont.reservationvol.Domaine.OTD.VolOTD
import java.sql.ResultSet
import java.time.LocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository

@Repository
class VolsDAOImpl(private val bd: JdbcTemplate) : VolsDAO {

        companion object {
                private const val QUERY_TOUS_LES_VOLS =
                        """
                        SELECT * FROM vols 
                        JOIN trajets ON vols.trajet_id = trajets.id 
                        JOIN aéroports AS ap_deb ON trajets.id_aéroport_debut = ap_deb.id 
                        JOIN aéroports AS ap_fin ON trajets.id_aéroport_fin = ap_fin.id 
                        JOIN villes AS ville_debut ON ap_deb.ville_id = ville_debut.id 
                        JOIN villes AS ville_fin ON ap_fin.ville_id = ville_fin.id 
                        JOIN prix_par_classe ON vols.id = prix_par_classe.id_vol 
                        JOIN avions ON vols.avion_id = avions.id
                        ORDER BY vols.id;
                        """

                private const val QUERY_VOL_PAR_ID =
                        """
                        SELECT * FROM vols 
                        JOIN trajets ON vols.trajet_id = trajets.id 
                        JOIN aéroports AS ap_deb ON trajets.id_aéroport_debut = ap_deb.id 
                        JOIN aéroports AS ap_fin ON trajets.id_aéroport_fin = ap_fin.id 
                        JOIN villes AS ville_debut ON ap_deb.ville_id = ville_debut.id 
                        JOIN villes AS ville_fin ON ap_fin.ville_id = ville_fin.id 
                        JOIN prix_par_classe ON vols.id = prix_par_classe.id_vol 
                        JOIN avions ON vols.avion_id = avions.id 
                        WHERE vols.id = ? 
                        ORDER BY vols.id;
                        """

                
                private const val QUERY_VOL_PAR_PARAM =
                        """
                        SELECT * FROM vols
                        JOIN trajets ON vols.trajet_id = trajets.id 
                        JOIN aéroports AS ap_deb ON trajets.id_aéroport_debut = ap_deb.id 
                        JOIN aéroports AS ap_fin ON trajets.id_aéroport_fin = ap_fin.id 
                        JOIN villes AS ville_debut ON ap_deb.ville_id = ville_debut.id 
                        JOIN villes AS ville_fin ON ap_fin.ville_id = ville_fin.id 
                        JOIN prix_par_classe ON vols.id = prix_par_classe.id_vol 
                        JOIN avions ON vols.avion_id = avions.id 
                        WHERE date_départ BETWEEN ? AND DATE_ADD(?, INTERVAL 30 DAY)
                        AND ap_deb.code = ? 
                        AND ap_fin.code = ?
                        ORDER BY vols.date_départ;
                        """

                private const val QUERY_SIEGE_PAR_VOL = """
                        SELECT * FROM sièges
                        JOIN vols_sièges ON vols_sièges.siège_id = sièges.id
                        JOIN vols ON vols_sièges.vol_id = vols.id
                        WHERE vols.id = ?
                        ORDER BY vols.id;
                        """

                private const val INSERT_DANS_VOLS_SIEGES = """
                    INSERT INTO vols_sièges (vol_id, siège_id, statut_siege) VALUES
                    (?,?,'disponible');
                    """

               private const val QUERY_VOL_EXISTANT = """
                   SELECT COUNT(*) FROM vols 
                   WHERE date_départ = ? 
                   AND date_arrivée = ? 
                   AND avion_id = ? 
                   AND trajet_id = ?
                   """

            private const val QUERY_VOLS_POUR_DEPART = """
               SELECT * FROM vols 
                        JOIN trajets ON vols.trajet_id = trajets.id 
                        JOIN aéroports AS ap_deb ON trajets.id_aéroport_debut = ap_deb.id 
                        JOIN aéroports AS ap_fin ON trajets.id_aéroport_fin = ap_fin.id 
                        JOIN villes AS ville_debut ON ap_deb.ville_id = ville_debut.id 
                        JOIN villes AS ville_fin ON ap_fin.ville_id = ville_fin.id 
                        JOIN prix_par_classe ON vols.id = prix_par_classe.id_vol 
                        JOIN avions ON vols.avion_id = avions.id
                WHERE vols.date_départ <= ?
                  AND vols.id NOT IN (SELECT id_vol FROM vol_statut WHERE statut = 'depart')
                ORDER BY vols.date_départ;
            """

            private const val QUERY_VOLS_POUR_ARRIVEE = """
               SELECT * FROM vols 
                        JOIN trajets ON vols.trajet_id = trajets.id 
                        JOIN aéroports AS ap_deb ON trajets.id_aéroport_debut = ap_deb.id 
                        JOIN aéroports AS ap_fin ON trajets.id_aéroport_fin = ap_fin.id 
                        JOIN villes AS ville_debut ON ap_deb.ville_id = ville_debut.id 
                        JOIN villes AS ville_fin ON ap_fin.ville_id = ville_fin.id 
                        JOIN prix_par_classe ON vols.id = prix_par_classe.id_vol 
                        JOIN avions ON vols.avion_id = avions.id
                WHERE vols.date_arrivée <= ?
                          AND vols.id NOT IN (SELECT id_vol FROM vol_statut WHERE statut = 'arrivé')
                        ORDER BY vols.date_arrivée;
            """



        }
        private fun mapVol(réponse: ResultSet): Vol {
                var ville_debut =
                        Ville(
                                réponse.getInt("ville_debut.id"),
                                réponse.getString("ville_debut.nom"),
                                réponse.getString("ville_debut.pays"),
                                réponse.getString("ville_debut.url_photo")
                        )
                var ville_fin =
                        Ville(
                                réponse.getInt("ville_fin.id"),
                                réponse.getString("ville_fin.nom"),
                                réponse.getString("ville_fin.pays"),
                                réponse.getString("ville_fin.url_photo")
                        )
                var aéroport_debut =
                        Aeroport(
                                réponse.getInt("ap_deb.id"),
                                réponse.getString("ap_deb.code"),
                                réponse.getString("ap_deb.nom"),
                                ville_debut,
                                réponse.getString("ap_deb.addresse")
                        )
                var aéroport_fin =
                        Aeroport(
                                réponse.getInt("ap_fin.id"),
                                réponse.getString("ap_fin.code"),
                                réponse.getString("ap_fin.nom"),
                                ville_fin,
                                réponse.getString("ap_fin.addresse")
                        )

                var avion = Avion(réponse.getInt("avions.id"), réponse.getString("avions.type"))

                var trajet =
                        Trajet(
                                réponse.getInt("trajets.id"),
                                réponse.getString("trajets.numéro_trajet"),
                                aéroport_debut,
                                aéroport_fin
                        )

                var prix_par_classe = hashMapOf<String, Double>()
                prix_par_classe["économique"] = réponse.getDouble("prix_par_classe.prix_économique")
                prix_par_classe["affaire"] = réponse.getDouble("prix_par_classe.prix_affaire")
                prix_par_classe["première"] = réponse.getDouble("prix_par_classe.prix_première")

                val volStatuts =
                        bd.query(
                                "SELECT * FROM vol_statut WHERE id_vol = ?;",
                                réponse.getInt("id_vol")
                        ) { réponseStatut, _ ->
                                VolStatut(
                                        réponseStatut.getInt("vol_statut.id_vol"),
                                        réponseStatut.getString("vol_statut.statut"),
                                        réponseStatut
                                                .getTimestamp("vol_statut.heure")
                                                .toLocalDateTime()
                                )
                        }
                val volSièges = listOf<Siège>()

                return Vol(
                        réponse.getInt("id"),
                        réponse.getTimestamp("date_départ").toLocalDateTime(),
                        réponse.getTimestamp("date_arrivée").toLocalDateTime(),
                        avion,
                        prix_par_classe,
                        réponse.getInt("poids_max_bag"),
                        trajet,
                        volStatuts,
                        réponse.getInt("durée").toDuration(DurationUnit.MINUTES).toJavaDuration(),
                        volSièges
                )
        }

        override fun chercherTous(): List<Vol> =
                bd.query(QUERY_TOUS_LES_VOLS) { réponse, _ -> mapVol(réponse) }

        override fun chercherParId(id: Int): Vol? =
                bd.query(QUERY_VOL_PAR_ID, id) { réponse, _ -> mapVol(réponse) }.singleOrNull()

        override fun effacer(id: Int) {
                bd.update("DELETE FROM vols WHERE id = ?", id)
        }
  
      
    override fun obtenirVolParParam(dateDebut: LocalDateTime, aeroportDebut: String, aeroportFin: String): List<Vol> {
            return bd.query(QUERY_VOL_PAR_PARAM, dateDebut ,dateDebut, aeroportDebut, aeroportFin) { réponse, _ ->
                mapVol(réponse)
            }
        }

    override fun ajouterVol(vol: Vol): Vol {
        val sql = """
            INSERT INTO vols (date_départ, date_arrivée, avion_id,trajet_id, poids_max_bag, durée)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        bd.update(sql, vol.dateDepart, vol.dateArrivee, vol.avion.id,vol.trajet.id, vol.poidsMaxBag, vol.duree.toMinutes())

        val nouvelId = bd.queryForObject("SELECT LAST_INSERT_ID()", Int::class.java) ?: throw Exception("Erreur lors de l'ajout du vol")


        for(i in 1..72){
            bd.update(INSERT_DANS_VOLS_SIEGES, nouvelId, i)
        }
        return vol.copy(id = nouvelId)
}

    override fun chercherVolsPourDepart(dateActuelle: LocalDateTime): List<Vol> {

        return bd.query(QUERY_VOLS_POUR_DEPART, { rs, _ -> mapVol(rs) }, dateActuelle)

    }

    override fun chercherVolsPourArrive(dateActuelle: LocalDateTime): List<Vol> {

        return bd.query(QUERY_VOLS_POUR_ARRIVEE, { rs, _ -> mapVol(rs) }, dateActuelle)

    }

    override fun ajouterStatutVol(volId: Int, statut: VolStatut) {
        val sql = "INSERT INTO vol_statut (id_vol, statut, heure) VALUES (?, ?, ?)"
        bd.update(sql, volId, statut.statut, statut.heure)
    }

    override fun ajouterPrixParClasse(volId: Int, prixParClasse: Map<String, Double>) {
        val sql = "INSERT INTO prix_par_classe (id_vol, prix_économique, prix_affaire, prix_première) VALUES (?, ?, ?, ?)"
        bd.update(
            sql,
            volId,
            prixParClasse["économique"],
            prixParClasse["affaire"],
            prixParClasse["première"]
        )
    }

    override fun trajetExiste(id: Int): Boolean {
        val sql = "SELECT COUNT(*) FROM trajets WHERE id = ?"
        return bd.queryForObject(sql, arrayOf(id), Int::class.java) ?: 0 > 0
    }

    override fun avionExiste(id: Int): Boolean {
        val sql = "SELECT COUNT(*) FROM avions WHERE id = ?"
        return bd.queryForObject(sql, arrayOf(id), Int::class.java) ?: 0 > 0
    }

    override fun volExiste(vol: Vol): Boolean {
        val count = bd.queryForObject(
                QUERY_VOL_EXISTANT,
                arrayOf(vol.dateDepart, vol.dateArrivee, vol.avion.id, vol.trajet.id),
                Int::class.java
        )
        return count != null && count > 0
    }


    override fun modifierVol(id: Int, modifieVol: Vol): Vol {
        val sql = """
            UPDATE vols 
            SET date_départ = ?, date_arrivée = ?, avion_id = ?, trajet_id = ?, poids_max_bag = ?, durée = ?
            WHERE id = ?
        """
        bd.update(
                sql,
                modifieVol.dateDepart,
                modifieVol.dateArrivee,
                modifieVol.avion.id,
                modifieVol.trajet.id,
                modifieVol.poidsMaxBag,
                modifieVol.duree.toMinutes(),
                id
        )

        val prixSql = """
            UPDATE prix_par_classe 
            SET prix_économique = ?, prix_affaire = ?, prix_première = ?
            WHERE id_vol = ?
        """
        bd.update(
                prixSql,
                modifieVol.prixParClasse["économique"],
                modifieVol.prixParClasse["affaire"],
                modifieVol.prixParClasse["première"],
                id
        )

        val deleteStatutsSql = "DELETE FROM vol_statut WHERE id_vol = ?"
        bd.update(deleteStatutsSql, id)
        modifieVol.vol_statut.forEach { statut ->
            ajouterStatutVol(id, statut)
        }

        return chercherParId(id) ?: throw RessourceInexistanteException("Le vol avec l'ID $id n'existe pas.")
    }




    override fun obtenirSiegeParVolId(id: Int): List<Siège> =
            bd.query(
                    QUERY_SIEGE_PAR_VOL,
                    id
            ){ réponseSiège, _ ->
                Siège(
                        réponseSiège.getInt("sièges.id"),
                        réponseSiège.getString("sièges.numéro_siège"),
                        réponseSiège.getString("sièges.classe"),
                        réponseSiège.getString("vols_sièges.statut_siege")
                )
            }



}

