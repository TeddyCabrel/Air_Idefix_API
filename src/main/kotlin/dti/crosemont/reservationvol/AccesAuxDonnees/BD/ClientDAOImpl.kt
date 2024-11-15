package dti.crosemont.reservationvol.AccesAuxDonnees.BD
import dti.crosemont.reservationvol.AccesAuxDonnees.SourcesDeDonnees.ClientDAO
import org.springframework.stereotype.Repository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import dti.crosemont.reservationvol.Domaine.Modele.Client
import java.sql.ResultSet

@Repository
class ClientDAOImpl( private val bd : JdbcTemplate ) : ClientDAO {
    companion object {
        private const val OBTENIR_TOUT_LES_CLIENTS : String = "SELECT * FROM clients;"
        private const val OBTENIR_CLIENT_PAR_ID : String = "SELECT * FROM clients WHERE id = ?;"
        private const val OBTENIR_CLIENT_PAR_MOT_CLÉ : String = 
            """
            SELECT * FROM clients WHERE prénom LIKE ? OR nom LIKE ?;
            """
        private const val AJOUTER_CLIENT : String =
                """
                INSERT INTO clients (nom, prénom, numéro_passeport, addresse, email, numéro_téléphone)
                VALUES (?, ?, ?, ?, ?, ?);
                """
        private const val OBTENIR_DERNIER_CLIENT_INSÉRER =
                """
                    SELECT * FROM clients 
                    WHERE id = ( SELECT MAX(id) from clients  );            
                """
    }

    override fun chercherTous(): List<Client> =
        bd.query( OBTENIR_TOUT_LES_CLIENTS ) {réponse, _ -> convertirRésultatEnClient(réponse)}

    override fun chercherParId(id: Int): Client? =
        bd.query( OBTENIR_CLIENT_PAR_ID, id )
            { réponse, _ -> convertirRésultatEnClient(réponse) }.singleOrNull()
    
    override fun chercherParMotCle(motClé: String): List<Client> =
        bd.query( OBTENIR_CLIENT_PAR_MOT_CLÉ, "$motClé%", "$motClé%" ) { réponse, _ -> convertirRésultatEnClient(réponse)}

    override fun effacer( id: Int ) { }

    override fun ajouter(client : Client) : Client?{
        var insertedClient : Client? = null

        val result = bd.update( AJOUTER_CLIENT,
                client.nom,
                client.prénom,
                client.numéroPasseport,
                client.adresse,
                client.email,
                client.numéroTéléphone )

        if( result != 0 ){
            insertedClient = bd.query(OBTENIR_DERNIER_CLIENT_INSÉRER)
                { réponse, _ -> convertirRésultatEnClient( réponse ) }.singleOrNull()
        }

        return insertedClient
    }

    private fun convertirRésultatEnClient( réponse : ResultSet ) : Client {
        return Client(
            id = réponse.getInt( "id" ) , 
            nom = réponse.getString( "nom" ), 
            prénom = réponse.getString( "prénom" ), 
            adresse = réponse.getString( "addresse" ),
            numéroPasseport = réponse.getString( "numéro_passeport" ),
            email = réponse.getString( "email" ),
            numéroTéléphone = réponse.getString( "numéro_téléphone" )
        )
    }
}