package dti.crosemont.reservationvol.AccesAuxDonnees.BD

import dti.crosemont.reservationvol.AccesAuxDonnees.SourcesDeDonnees.ReservationsDAO
import dti.crosemont.reservationvol.Controleurs.Exceptions.RessourceInexistanteException
import dti.crosemont.reservationvol.Domaine.Modele.Reservation
import dti.crosemont.reservationvol.Domaine.Modele.Client
import dti.crosemont.reservationvol.Domaine.Modele.Siège
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository

@Repository
class ReservationsDAOImpl(private val bd: JdbcTemplate): ReservationsDAO {

        
    override fun chercherTous(): List<Reservation> {
        val query = "SELECT * FROM réservations"
        return bd.query(query) { reponse, _ ->
            Reservation(
            id = reponse.getInt("id"),
            numéroRéservation = reponse.getString("numéro_réservation"),
            idVol = reponse.getInt("id_vol"),
            client = obtenirClientPourRéservation(reponse.getInt("id_client")), 
            siège = obtenirSiègePourRéservation(reponse.getInt("id")),  
            classe = reponse.getString("classe"),
            bagages = reponse.getInt("bagages")
            )
        }
    }

    override fun chercherTous(idClient: Int): List<Reservation> =
    bd.query(" SELECT * FROM réservations WHERE id_client = ? ", idClient) { reponse, _ ->
        Reservation(
        id = reponse.getInt("id"),
        numéroRéservation = reponse.getString("numéro_réservation"),
        idVol = reponse.getInt("id_vol"),
        client = obtenirClientPourRéservation(reponse.getInt("id_client")), 
        siège = obtenirSiègePourRéservation(reponse.getInt("id")),  
        classe = reponse.getString("classe"),
        bagages = reponse.getInt("bagages")
        )
    }

    
    fun verifierSiègeDisponible(idVol: Int, idSiège: Int): Boolean {
        val query = "SELECT statut_siege FROM vols_sièges WHERE vol_id = ? AND siège_id = ?"
        return bd.queryForObject(query, arrayOf(idVol, idSiège), String::class.java) == "disponible"
    }

    fun obtenirClientPourRéservation(idClient: Int): Client {
        val query = "SELECT * FROM clients WHERE id = ?"
        return bd.query(query, arrayOf(idClient)) { reponse, _ ->
            Client(
            id = reponse.getInt("id"),
            nom = reponse.getString("nom"),
            prénom = reponse.getString("prénom"),
            numéroPasseport = reponse.getString("numéro_passeport"),
            adresse = reponse.getString("addresse"),
            email = reponse.getString("email"),
            numéroTéléphone = reponse.getString("numéro_téléphone")
            )
        }.firstOrNull() ?: throw RessourceInexistanteException("Client avec l'ID $idClient est introuvable")
    }


    private fun obtenirSiègePourRéservation(réservationId: Int): Siège {
                val query = """
                    SELECT sièges.* 
                    FROM sièges
                    JOIN réservations ON sièges.id = réservations.id_sièges
                    WHERE réservations.id = ?
                    """
        return bd.queryForObject(query, arrayOf(réservationId)) { reponse, _ ->
            Siège(
                id = reponse.getInt("id"),
                numéroSiège = reponse.getString("numéro_siège"),
                classe = reponse.getString("classe")
                )
            } ?: throw IllegalArgumentException("Siège non trouvé pour la réservation $réservationId")
    }


    private fun obtenirSiègeIdParNuméro(numéroSiège: String, classeSiège: String): Int {
        val query = "SELECT id FROM sièges WHERE numéro_siège = ? AND classe = ?"
        return bd.queryForObject(query, arrayOf(numéroSiège, classeSiège), Int::class.java)
        ?: throw IllegalArgumentException("Siège non trouvé avec le numéro $numéroSiège")
    }



    override fun ajouterRéservation(réservation: Reservation): Reservation {
        
        if (!verifierSiègeDisponible(réservation.idVol, réservation.siège.id)) {
            throw IllegalArgumentException("Le siège sélectionné n'est pas disponible.")
        }

        modifierSiègeVol(réservation)

        val query = """
            INSERT INTO réservations 
            (numéro_réservation, id_vol, classe, id_sièges, id_client, bagages) 
            VALUES (?, ?, ?, ?, ?, ?)
            """
        val siegeId = obtenirSiègeIdParNuméro(réservation.siège.numéroSiège, réservation.classe)

        bd.update(
            query,
            réservation.numéroRéservation,
            réservation.idVol,
            réservation.classe,
            siegeId,
            réservation.client.id,
            réservation.bagages
        )

        val réservationId = bd.queryForObject("SELECT LAST_INSERT_ID()", Int::class.java)
            ?: throw IllegalArgumentException("L'ID de la réservation est nul")

    return chercherParId(réservationId)!!
    }



    override fun chercherParId(id: Int): Reservation? {
        val query = "SELECT * FROM réservations WHERE id = ?"
        return bd.query(query, arrayOf(id)) { reponse, _ ->
        Reservation(
            id = reponse.getInt("id"),
            numéroRéservation = reponse.getString("numéro_réservation"),
            idVol = reponse.getInt("id_vol"),
            client = obtenirClientPourRéservation(reponse.getInt("id_client")), 
            siège = obtenirSiègePourRéservation(reponse.getInt("id")), 
            classe = reponse.getString("classe"),
            bagages = reponse.getInt("bagages")
            )
        }.firstOrNull()
    }


    override fun modifierRéservation(id: Int, réservation: Reservation): Reservation {
        bd.update(
            """
            UPDATE réservations SET
            numéro_réservation = ?,
            id_vol = ?,
            classe = ?,
            bagages = ?,
            id_sièges = ? 
            WHERE id = ?
            """,
            réservation.numéroRéservation,
            réservation.idVol,
            réservation.classe,
            réservation.bagages,
            réservation.siège.id, 
            id
        )
        return this.chercherParId(id)!! 
    }


    override fun effacer(id: Int) {
        bd.update("DELETE FROM réservations WHERE id = ?", id)
    }

    override fun modifierSiègeVol( réservation: Reservation ) {
        bd.update(
            """
            UPDATE vols_sièges SET
            statut_siege = ?
            WHERE vol_id = ?
            AND
            siège_id = ?
            """,
            réservation.siège.statut,
            réservation.idVol,
            réservation.siège.id
        )
    }
}